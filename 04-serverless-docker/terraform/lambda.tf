# Build the jar first: cd ../lambda && mvn package
resource "aws_lambda_function" "students" {
  function_name    = "students-api"
  role             = data.aws_iam_role.lab.arn
  runtime          = "java17"
  handler          = "ec.uce.pd.lambda.StudentHandler::handleRequest"
  filename         = "${path.module}/../lambda/target/students-lambda.jar"
  source_code_hash = filebase64sha256("${path.module}/../lambda/target/students-lambda.jar")
  memory_size      = 512
  timeout          = 15
}

resource "aws_apigatewayv2_api" "students" {
  name          = "students-http-api"
  protocol_type = "HTTP"
}

resource "aws_apigatewayv2_integration" "lambda" {
  api_id                 = aws_apigatewayv2_api.students.id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.students.invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "get_student" {
  api_id    = aws_apigatewayv2_api.students.id
  route_key = "GET /students/{id}"
  target    = "integrations/${aws_apigatewayv2_integration.lambda.id}"
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.students.id
  name        = "$default"
  auto_deploy = true
}

resource "aws_lambda_permission" "apigw" {
  statement_id  = "AllowApiGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.students.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.students.execution_arn}/*/*"
}
