# Cloud Architecture and Design Patterns (Java)

Teaching implementations of cloud architecture patterns and AWS technologies, built with Java and deployed for real on AWS where the topic requires it.
Each topic lives in its own folder and its own branch, with a `README.md` (concepts, design, how to run, evidences) and an `evidences/` folder with the raw outputs and screenshots.

Course: Distributed Programming, Universidad Central del Ecuador. Student: Chiluisa Alexis.

## Topics

Branches are **sequential**: each one is created from the previous one, so the last branch contains everything.

| #   | Branch                                  | Topic                                     | Type                                  | Folder                                                          |
| --- | --------------------------------------- | ----------------------------------------- | ------------------------------------- | --------------------------------------------------------------- |
| 01  | `feature/01-circuit-breaker`            | Circuit Breaker                           | Spring Boot + Resilience4j (local)    | [01-circuit-breaker](01-circuit-breaker/)                       |
| 02  | `feature/02-retry`                      | Retry with exponential backoff            | Spring Boot + Resilience4j (local)    | [02-retry](02-retry/)                                           |
| 03  | `feature/03-timeout`                    | Timeout                                   | Spring Boot (local)                   | [03-timeout](03-timeout/)                                       |
| 04  | `feature/04-serverless-docker`          | Lambda vs. ECS (Fargate)                  | Java + Terraform on AWS               | [04-serverless-docker](04-serverless-docker/)                   |
| 05  | `feature/05-gateway-loadbalancer-proxy` | API Gateway, Load Balancer, Reverse Proxy | Spring Boot + Terraform on AWS        | [05-gateway-loadbalancer-proxy](05-gateway-loadbalancer-proxy/) |
| 06  | `feature/06-hub-and-spoke`              | Hub-and-Spoke                             | Terraform on AWS (Transit Gateway)    | [06-hub-and-spoke](06-hub-and-spoke/)                           |
| 07  | `feature/07-lift-and-shift`             | Lift-and-Shift                            | Spring Boot + PostgreSQL + Terraform  | [07-lift-and-shift](07-lift-and-shift/)                         |
| 08  | `feature/08-bastion`                    | Bastion Host                              | Terraform on AWS + test script        | [08-bastion](08-bastion/)                                       |

What each topic demonstrates, with the evidence collected:

| #   | Key result                                                                                                                    |
| --- | ----------------------------------------------------------------------------------------------------------------------------- |
| 01  | The circuit opens after repeated failures, rejects calls immediately and recovers through a half-open probe.                  |
| 02  | Transient failures are absorbed with exponential backoff; a permanent error (404) is not retried.                             |
| 03  | A slow call is cut at 2 s (HTTP 504); the late response is discarded, which raises the idempotency question.                  |
| 04  | Lambda cold start (about 1.6 s) against a warm average of about 140 ms; ECS stays steady with no cold start.                  |
| 05  | The API Gateway throttles bursts (HTTP 429); the ALB spreads requests across two tasks (141 / 141).                           |
| 06  | Spokes reach the hub but not each other; a single route-table switch opens spoke-to-spoke traffic.                            |
| 07  | The application and its database were rehosted to EC2 with a measured interruption of about 155 s.                            |
| 08  | The private server is unreachable directly, reachable through the bastion, and manageable with Systems Manager without SSH.   |

## Requirements

| Tool                           | Used by    | Notes                                                                       |
| ------------------------------ | ---------- | --------------------------------------------------------------------------- |
| JDK 17 and Maven               | 01 to 07   | Each topic's README says which module to run from which folder.             |
| Docker                         | 04, 05, 07 | Image builds, local database and the container registry push.               |
| Terraform and AWS CLI          | 04 to 08   | The AWS topics are deployed for real.                                       |
| OpenSSH client                 | 08         | Included in Windows 10 and 11.                                              |
| AWS Academy Learner Lab        | 04 to 08   | Temporary credentials, no IAM role creation, `us-east-1` or `us-west-2`.    |

## AWS Academy Learner Lab notes

- Credentials are temporary (about 4 hours). After restarting the lab, copy the new ones to `%USERPROFILE%\.aws\credentials`.
- IAM roles cannot be created: the topics use the pre-created `LabRole` and `LabInstanceProfile`.
- Some actions are denied by the lab policy (for example S3 object-lock queries), so the 07 bucket is created with the AWS CLI.
- Every AWS topic ends with `terraform destroy` to stop the charges. All resources were destroyed after collecting the evidences.

## How to review

1. Pick a topic in the table and open its folder.
2. Read its `README.md`: concepts, design, how to run.
3. The **Evidences** section at the end of each README shows the real outputs and screenshots, which are also stored in the topic's `evidences/` folder.

## Conventions

- Commits follow [Conventional Commits](https://www.conventionalcommits.org/) with a description body.
- Documentation and commit messages are written in English.
- Build output, Terraform state and credentials are never tracked (see `.gitignore`).
