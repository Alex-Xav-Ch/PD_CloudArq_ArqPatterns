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

variable "admin_cidr" {
  description = "Public IP of the administrator in CIDR form, e.g. 203.0.113.10/32. Only this address can reach the bastion."
  type        = string
}

variable "key_name" {
  description = "Existing EC2 key pair. Learner Lab provides one called vockey (labsuser.pem)."
  type        = string
  default     = "vockey"
}

variable "enable_ssm" {
  description = "Also manage the private server with Systems Manager, without SSH (adds three VPC endpoints)."
  type        = bool
  default     = false
}

data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023*-kernel-6.1-x86_64"]
  }
}

output "bastion_public_ip" {
  value = aws_instance.bastion.public_ip
}

output "bastion_id" {
  value = aws_instance.bastion.id
}

output "private_server_ip" {
  value = aws_instance.private.private_ip
}

output "private_server_id" {
  value = aws_instance.private.id
}
