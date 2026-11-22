# Cloud Architecture and Design Patterns (Java)

Java version of the teaching implementations of cloud architecture patterns and AWS technologies.
Each topic lives in its own branch with its own `README.md` (concepts, design, how to run) and an `evidences/` folder.

Stack: Java 17, Spring Boot 3.4, Resilience4j, Maven.

## Branches

Branches are **sequential**: each one is created from the previous one, so the last branch contains everything.

| #   | Branch                                  | Topic                                     |
| --- | --------------------------------------- | ----------------------------------------- |
| 01  | `feature/01-circuit-breaker`            | Circuit Breaker                           |
| 02  | `feature/02-retry`                      | Retry with exponential backoff            |
| 03  | `feature/03-timeout`                    | Timeout                                   |
| 04  | `feature/04-serverless-docker`          | Lambda vs. ECS (Fargate)                  |
| 05  | `feature/05-gateway-loadbalancer-proxy` | API Gateway, Load Balancer, Reverse Proxy |
| 06  | `feature/06-hub-and-spoke`              | Hub-and-Spoke                             |
| 07  | `feature/07-lift-and-shift`             | Lift-and-Shift                            |
| 08  | `feature/08-bastion`                    | Bastion Host                              |

## Conventions

- Commits follow [Conventional Commits](https://www.conventionalcommits.org/) with a description body.
- Documentation and commit messages are written in English.
