# No inbound port is open except ping from the private 10.x ranges. Instances are managed with Systems Manager.
resource "aws_security_group" "test" {
  for_each = local.vpcs
  name     = "${each.key}-test-sg"
  vpc_id   = aws_vpc.this[each.key].id

  ingress {
    description = "ping from the private ranges"
    from_port   = -1
    to_port     = -1
    protocol    = "icmp"
    cidr_blocks = ["10.0.0.0/8"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Learner Lab provides LabInstanceProfile, which lets the instance register in Systems Manager.
resource "aws_instance" "test" {
  for_each               = local.vpcs
  ami                    = data.aws_ami.al2023.id
  instance_type          = "t3.micro"
  subnet_id              = aws_subnet.main[each.key].id
  vpc_security_group_ids = [aws_security_group.test[each.key].id]
  iam_instance_profile   = "LabInstanceProfile"
  tags                   = { Name = "${each.key}-test" }
}
