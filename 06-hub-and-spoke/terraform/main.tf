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

variable "allow_spoke_to_spoke" {
  description = "false: spokes only reach the hub. true: spokes can also reach each other through the Transit Gateway."
  type        = bool
  default     = false
}

locals {
  vpcs = {
    hub       = "10.0.0.0/16" # shared services
    spoke_app = "10.1.0.0/16" # applications
    spoke_dev = "10.2.0.0/16" # development
  }
  spokes = [for name in keys(local.vpcs) : name if name != "hub"]
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

output "instance_ids" {
  value = { for k, i in aws_instance.test : k => i.id }
}

output "private_ips" {
  value = { for k, i in aws_instance.test : k => i.private_ip }
}

output "tgw_route_tables" {
  description = "Ids of the two Transit Gateway route tables, to inspect their routes"
  value = {
    hub    = aws_ec2_transit_gateway_route_table.hub.id
    spokes = aws_ec2_transit_gateway_route_table.spokes.id
  }
}
