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

variable "container_image" {
  description = "ECR image URI. Empty on the first apply: the ECS resources are created only when it is set."
  type        = string
  default     = ""
}

locals {
  deploy_ecs = var.container_image != ""
}

# Learner Lab does not allow creating IAM roles: every service reuses the pre-created LabRole.
data "aws_iam_role" "lab" {
  name = "LabRole"
}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

output "lambda_api_url" {
  description = "Base URL of the Lambda version (append /students/1)"
  value       = aws_apigatewayv2_api.students.api_endpoint
}

output "ecr_repository_url" {
  description = "Where to push the Docker image"
  value       = aws_ecr_repository.students.repository_url
}

output "ecs_alb_url" {
  description = "Base URL of the ECS/Fargate version (append /students/1). Null until container_image is set."
  value       = try("http://${aws_lb.students[0].dns_name}", null)
}
