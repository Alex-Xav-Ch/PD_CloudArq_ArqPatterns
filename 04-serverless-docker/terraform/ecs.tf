resource "aws_ecr_repository" "students" {
  name         = "students-api"
  force_delete = true
}

# Everything below is created only when var.container_image is set (second apply).

resource "aws_cloudwatch_log_group" "ecs" {
  count             = local.deploy_ecs ? 1 : 0
  name              = "/ecs/students-api"
  retention_in_days = 3
}

resource "aws_ecs_cluster" "main" {
  count = local.deploy_ecs ? 1 : 0
  name  = "students-cluster"
}

resource "aws_security_group" "alb" {
  count  = local.deploy_ecs ? 1 : 0
  name   = "students-alb-sg"
  vpc_id = data.aws_vpc.default.id

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
  count  = local.deploy_ecs ? 1 : 0
  name   = "students-task-sg"
  vpc_id = data.aws_vpc.default.id

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

resource "aws_lb" "students" {
  count              = local.deploy_ecs ? 1 : 0
  name               = "students-alb"
  load_balancer_type = "application"
  subnets            = data.aws_subnets.default.ids
  security_groups    = [aws_security_group.alb[0].id]
}

resource "aws_lb_target_group" "students" {
  count       = local.deploy_ecs ? 1 : 0
  name        = "students-tg"
  port        = 8080
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = data.aws_vpc.default.id

  health_check {
    path                = "/health"
    interval            = 15
    healthy_threshold   = 2
    unhealthy_threshold = 3
    matcher             = "200"
  }
}

resource "aws_lb_listener" "http" {
  count             = local.deploy_ecs ? 1 : 0
  load_balancer_arn = aws_lb.students[0].arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.students[0].arn
  }
}

resource "aws_ecs_task_definition" "students" {
  count                    = local.deploy_ecs ? 1 : 0
  family                   = "students-api"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = data.aws_iam_role.lab.arn
  task_role_arn            = data.aws_iam_role.lab.arn

  container_definitions = jsonencode([{
    name         = "students-api"
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

resource "aws_ecs_service" "students" {
  count           = local.deploy_ecs ? 1 : 0
  name            = "students-service"
  cluster         = aws_ecs_cluster.main[0].id
  task_definition = aws_ecs_task_definition.students[0].arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.task[0].id]
    assign_public_ip = true # needed to pull the image from ECR without a NAT gateway
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.students[0].arn
    container_name   = "students-api"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.http]
}
