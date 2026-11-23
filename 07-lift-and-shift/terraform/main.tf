terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.0" }
  }
}

# Credentials come from ~/.aws/credentials (AWS Academy Learner Lab "AWS Details").
provider "aws" {
  region = var.region
}

variable "region" {
  description = "Learner Lab only allows us-east-1 and us-west-2"
  type        = string
  default     = "us-east-1"
}

variable "migrate" {
  description = "false: nothing is created yet. true: the target server is created (after the files are uploaded to S3)."
  type        = bool
  default     = false
}

variable "instance_type" {
  type    = string
  default = "t3.small"
}

data "aws_caller_identity" "current" {}

data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "az" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
  filter {
    name   = "availability-zone"
    values = [data.aws_availability_zones.available.names[0]]
  }
}

data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023*-kernel-6.1-x86_64"]
  }
}

# Staging area: the database dump and the application jar travel from the origin to the target through S3.
# The bucket is created with the AWS CLI (see the readme): the Learner Lab service control policy denies
# s3:GetBucketObjectLockConfiguration, which the Terraform AWS provider calls whenever it manages a bucket.
locals {
  bucket = "lift-and-shift-${data.aws_caller_identity.current.account_id}-${var.region}"
}

resource "aws_security_group" "target" {
  count  = var.migrate ? 1 : 0
  name   = "lift-and-shift-target-sg"
  vpc_id = data.aws_vpc.default.id

  ingress {
    description = "Application"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# The target is a server with the same stack as the origin: Java and PostgreSQL on one machine.
# Learner Lab provides LabInstanceProfile, which lets the instance read from S3.
resource "aws_instance" "target" {
  count                       = var.migrate ? 1 : 0
  ami                         = data.aws_ami.al2023.id
  instance_type               = var.instance_type
  subnet_id                   = data.aws_subnets.az.ids[0]
  vpc_security_group_ids      = [aws_security_group.target[0].id]
  iam_instance_profile        = "LabInstanceProfile"
  associate_public_ip_address = true
  user_data                   = templatefile("${path.module}/user_data.sh.tftpl", { bucket = local.bucket })
  tags                        = { Name = "lift-and-shift-target" }
}

output "bucket_name" {
  value = local.bucket
}

output "app_url" {
  description = "Base URL of the migrated application. Null until migrate = true."
  value       = try("http://${aws_instance.target[0].public_ip}:8080", null)
}

output "target_private_ip" {
  value = try(aws_instance.target[0].private_ip, null)
}

output "target_instance_id" {
  value = try(aws_instance.target[0].id, null)
}
