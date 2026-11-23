# Three VPCs, each with one subnet and an Internet Gateway (only so that Systems Manager can reach the instances).
resource "aws_vpc" "this" {
  for_each             = local.vpcs
  cidr_block           = each.value
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags                 = { Name = each.key }
}

resource "aws_internet_gateway" "this" {
  for_each = local.vpcs
  vpc_id   = aws_vpc.this[each.key].id
  tags     = { Name = "${each.key}-igw" }
}

resource "aws_subnet" "main" {
  for_each                = local.vpcs
  vpc_id                  = aws_vpc.this[each.key].id
  cidr_block              = cidrsubnet(each.value, 8, 1)
  availability_zone       = data.aws_availability_zones.available.names[0]
  map_public_ip_on_launch = true
  tags                    = { Name = "${each.key}-subnet" }
}

# All the routes are separate aws_route resources. Do not add inline `route` blocks here: a table with inline
# routes removes the aws_route routes (such as the one to the Transit Gateway) on the next apply.
resource "aws_route_table" "main" {
  for_each = local.vpcs
  vpc_id   = aws_vpc.this[each.key].id
  tags     = { Name = "${each.key}-rt" }
}

resource "aws_route" "to_internet" {
  for_each               = local.vpcs
  route_table_id         = aws_route_table.main[each.key].id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.this[each.key].id
}

resource "aws_route_table_association" "main" {
  for_each       = local.vpcs
  subnet_id      = aws_subnet.main[each.key].id
  route_table_id = aws_route_table.main[each.key].id
}

# All the private 10.x traffic leaves each VPC through the Transit Gateway.
# The local route of each VPC (/16) is more specific, so traffic inside a VPC never goes through it.
resource "aws_route" "to_tgw" {
  for_each               = local.vpcs
  route_table_id         = aws_route_table.main[each.key].id
  destination_cidr_block = "10.0.0.0/8"
  transit_gateway_id     = aws_ec2_transit_gateway.hub.id
  depends_on             = [aws_ec2_transit_gateway_vpc_attachment.this]
}
