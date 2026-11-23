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
  description = "ECR image URI. Empty on the first apply: ECS, ALB and API Gateway are created only when it is set."
  type        = string
  default     = ""
}

variable "throttling_rate_limit" {
  description = "API Gateway steady-state requests per second"
  type        = number
  default     = 2
}

variable "throttling_burst_limit" {
  description = "API Gateway burst capacity"
  type        = number
  default     = 2
}

locals {
  deploy = var.container_image != ""
}

# Learner Lab does not allow creating IAM roles: every service reuses the pre-created LabRole.
data "aws_iam_role" "lab" {
  name = "LabRole"
}

data "aws_availability_zones" "available" {
  state = "available"
}

output "ecr_repository_url" {
  description = "Where to push the Docker image"
  value       = aws_ecr_repository.whoami.repository_url
}

output "api_gateway_url" {
  description = "Entry point with throttling (append /whoami). Null until container_image is set."
  value       = try(aws_apigatewayv2_api.gateway[0].api_endpoint, null)
}

output "alb_url" {
  description = "Load balancer reached directly, without the gateway (append /whoami). Null until container_image is set."
  value       = try("http://${aws_lb.main[0].dns_name}", null)
}
