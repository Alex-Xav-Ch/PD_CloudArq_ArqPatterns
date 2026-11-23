resource "aws_ecr_repository" "whoami" {
  name         = "whoami-api"
  force_delete = true
}

# Everything below is created only when var.container_image is set (second apply).

resource "aws_cloudwatch_log_group" "ecs" {
  count             = local.deploy ? 1 : 0
  name              = "/ecs/whoami-api"
  retention_in_days = 3
}

resource "aws_ecs_cluster" "main" {
  count = local.deploy ? 1 : 0
  name  = "whoami-cluster"
}

resource "aws_security_group" "alb" {
  count  = local.deploy ? 1 : 0
  name   = "whoami-alb-sg"
  vpc_id = aws_vpc.main.id

  ingress {
    from_port   = 80
    to_port     = 80
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

# The tasks only accept traffic coming from the load balancer.
resource "aws_security_group" "task" {
  count  = local.deploy ? 1 : 0
  name   = "whoami-task-sg"
  vpc_id = aws_vpc.main.id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb[0].id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_lb" "main" {
  count              = local.deploy ? 1 : 0
  name               = "whoami-alb"
  load_balancer_type = "application"
  subnets            = aws_subnet.public[*].id
  security_groups    = [aws_security_group.alb[0].id]
}

resource "aws_lb_target_group" "main" {
  count       = local.deploy ? 1 : 0
  name        = "whoami-tg"
  port        = 8080
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.main.id

  health_check {
    path                = "/health"
    interval            = 15
    healthy_threshold   = 2
    unhealthy_threshold = 3
    matcher             = "200"
  }
}

resource "aws_lb_listener" "http" {
  count             = local.deploy ? 1 : 0
  load_balancer_arn = aws_lb.main[0].arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.main[0].arn
  }
}

resource "aws_ecs_task_definition" "whoami" {
  count                    = local.deploy ? 1 : 0
  family                   = "whoami-api"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = data.aws_iam_role.lab.arn
  task_role_arn            = data.aws_iam_role.lab.arn

  container_definitions = jsonencode([{
    name         = "whoami-api"
    image        = var.container_image
    essential    = true
    portMappings = [{ containerPort = 8080 }]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.ecs[0].name
        awslogs-region        = var.region
        awslogs-stream-prefix = "ecs"
      }
    }
  }])
}

# Two tasks: the load balancer spreads the requests between them.
resource "aws_ecs_service" "whoami" {
  count           = local.deploy ? 1 : 0
  name            = "whoami-service"
  cluster         = aws_ecs_cluster.main[0].id
  task_definition = aws_ecs_task_definition.whoami[0].arn
  desired_count   = 2
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.task[0].id]
    assign_public_ip = true # needed to pull the image from ECR without a NAT gateway
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.main[0].arn
    container_name   = "whoami-api"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.http]
}
