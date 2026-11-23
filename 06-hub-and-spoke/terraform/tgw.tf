# The Transit Gateway is the hub. Default association and propagation are disabled:
# who can reach whom is decided only by the two route tables below.
resource "aws_ec2_transit_gateway" "hub" {
  description                     = "Hub-and-spoke transit gateway"
  default_route_table_association = "disable"
  default_route_table_propagation = "disable"
  tags                            = { Name = "hub-tgw" }
}

resource "aws_ec2_transit_gateway_vpc_attachment" "this" {
  for_each           = local.vpcs
  transit_gateway_id = aws_ec2_transit_gateway.hub.id
  vpc_id             = aws_vpc.this[each.key].id
  subnet_ids         = [aws_subnet.main[each.key].id]

  transit_gateway_default_route_table_association = false
  transit_gateway_default_route_table_propagation = false

  tags = { Name = "${each.key}-attachment" }
}

# Used by the hub attachment: it learns the routes of every spoke.
resource "aws_ec2_transit_gateway_route_table" "hub" {
  transit_gateway_id = aws_ec2_transit_gateway.hub.id
  tags               = { Name = "hub-rt" }
}

# Used by the spoke attachments: it learns the hub route, and the other spokes only if allowed.
resource "aws_ec2_transit_gateway_route_table" "spokes" {
  transit_gateway_id = aws_ec2_transit_gateway.hub.id
  tags               = { Name = "spokes-rt" }
}

resource "aws_ec2_transit_gateway_route_table_association" "hub" {
  transit_gateway_attachment_id  = aws_ec2_transit_gateway_vpc_attachment.this["hub"].id
  transit_gateway_route_table_id = aws_ec2_transit_gateway_route_table.hub.id
}

resource "aws_ec2_transit_gateway_route_table_association" "spokes" {
  for_each                       = toset(local.spokes)
  transit_gateway_attachment_id  = aws_ec2_transit_gateway_vpc_attachment.this[each.key].id
  transit_gateway_route_table_id = aws_ec2_transit_gateway_route_table.spokes.id
}

resource "aws_ec2_transit_gateway_route_table_propagation" "spokes_into_hub_rt" {
  for_each                       = toset(local.spokes)
  transit_gateway_attachment_id  = aws_ec2_transit_gateway_vpc_attachment.this[each.key].id
  transit_gateway_route_table_id = aws_ec2_transit_gateway_route_table.hub.id
}

resource "aws_ec2_transit_gateway_route_table_propagation" "hub_into_spokes_rt" {
  transit_gateway_attachment_id  = aws_ec2_transit_gateway_vpc_attachment.this["hub"].id
  transit_gateway_route_table_id = aws_ec2_transit_gateway_route_table.spokes.id
}

# The switch: spokes learn each other's routes only when allow_spoke_to_spoke is true.
resource "aws_ec2_transit_gateway_route_table_propagation" "spokes_into_spokes_rt" {
  for_each                       = var.allow_spoke_to_spoke ? toset(local.spokes) : toset([])
  transit_gateway_attachment_id  = aws_ec2_transit_gateway_vpc_attachment.this[each.key].id
  transit_gateway_route_table_id = aws_ec2_transit_gateway_route_table.spokes.id
}
