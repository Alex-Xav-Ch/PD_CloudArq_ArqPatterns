# API Gateway in front of the load balancer. It adds what the ALB does not: request throttling.
resource "aws_apigatewayv2_api" "gateway" {
  count         = local.deploy ? 1 : 0
  name          = "whoami-http-api"
  protocol_type = "HTTP"
}

resource "aws_apigatewayv2_integration" "alb" {
  count                  = local.deploy ? 1 : 0
  api_id                 = aws_apigatewayv2_api.gateway[0].id
  integration_type       = "HTTP_PROXY"
  integration_method     = "GET"
  integration_uri        = "http://${aws_lb.main[0].dns_name}/whoami"
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_route" "whoami" {
  count     = local.deploy ? 1 : 0
  api_id    = aws_apigatewayv2_api.gateway[0].id
  route_key = "GET /whoami"
  target    = "integrations/${aws_apigatewayv2_integration.alb[0].id}"
}

resource "aws_apigatewayv2_stage" "default" {
  count       = local.deploy ? 1 : 0
  api_id      = aws_apigatewayv2_api.gateway[0].id
  name        = "$default"
  auto_deploy = true

  # Above this rate the gateway answers 429 Too Many Requests without reaching the ALB.
  default_route_settings {
    throttling_rate_limit  = var.throttling_rate_limit
    throttling_burst_limit = var.throttling_burst_limit
  }
}
