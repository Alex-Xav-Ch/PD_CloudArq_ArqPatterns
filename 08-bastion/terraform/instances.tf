resource "aws_security_group" "bastion" {
  name   = "bastion-sg"
  vpc_id = aws_vpc.main.id

  ingress {
    description = "SSH only from the administrator IP"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.admin_cidr]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "private" {
  name   = "private-server-sg"
  vpc_id = aws_vpc.main.id

  ingress {
    description     = "SSH only from the bastion security group"
    from_port       = 22
    to_port         = 22
    protocol        = "tcp"
    security_groups = [aws_security_group.bastion.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Learner Lab provides LabInstanceProfile, which also lets the instances register in Systems Manager.
resource "aws_instance" "bastion" {
  ami                    = data.aws_ami.al2023.id
  instance_type          = "t3.micro"
  subnet_id              = aws_subnet.public.id
  vpc_security_group_ids = [aws_security_group.bastion.id]
  key_name               = var.key_name
  iam_instance_profile   = "LabInstanceProfile"
  tags                   = { Name = "bastion" }
}

resource "aws_instance" "private" {
  ami                         = data.aws_ami.al2023.id
  instance_type               = "t3.micro"
  subnet_id                   = aws_subnet.private.id
  vpc_security_group_ids      = [aws_security_group.private.id]
  key_name                    = var.key_name
  iam_instance_profile        = "LabInstanceProfile"
  associate_public_ip_address = false
  tags                        = { Name = "private-server" }
}

# ---------- Optional: Systems Manager as an alternative to the bastion ----------
# The private subnet has no Internet access, so Systems Manager needs interface endpoints inside the VPC.

resource "aws_security_group" "endpoints" {
  count  = var.enable_ssm ? 1 : 0
  name   = "ssm-endpoints-sg"
  vpc_id = aws_vpc.main.id

  ingress {
    description = "HTTPS from inside the VPC"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = [aws_vpc.main.cidr_block]
  }
}

resource "aws_vpc_endpoint" "ssm" {
  for_each            = var.enable_ssm ? toset(["ssm", "ssmmessages", "ec2messages"]) : toset([])
  vpc_id              = aws_vpc.main.id
  service_name        = "com.amazonaws.${var.region}.${each.key}"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = [aws_subnet.private.id]
  security_group_ids  = [aws_security_group.endpoints[0].id]
  private_dns_enabled = true
}
