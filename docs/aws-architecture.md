# AWS Architecture — Payment Service (Design)

> **Status: design-only.** Nothing here is provisioned yet. This document is the target
> architecture and the migration order. Local development continues to use Docker Compose
> (Postgres) + LocalStack for emulating AWS services at zero cost.

## Goals

Take the current single-instance Spring Boot service and make it look and behave like a
service that runs in production: managed data, async processing, real auth, centralized
secrets, and full observability — deployed from CI/CD on container infrastructure.

## Target topology

```
                          ┌──────────────────────────────────────────────┐
   Browser (React SPA)    │                   AWS                          │
        │                 │                                                │
        │  HTTPS          │   ┌───────────┐      ┌──────────────────────┐  │
        ▼                 │   │ CloudFront│─────▶│ S3 (React static app)│  │
   ┌─────────┐  /api      │   └───────────┘      └──────────────────────┘  │
   │  CDN /  │────────────┼──▶┌───────────┐                                 │
   │  ALB    │            │   │    ALB    │                                 │
   └─────────┘            │   └─────┬─────┘                                 │
                          │         ▼                                       │
                          │   ┌───────────────┐   JWT validate  ┌─────────┐ │
                          │   │ ECS Fargate   │◀───────────────▶│ Cognito │ │
                          │   │ payment-svc   │                 └─────────┘ │
                          │   │ (Spring Boot) │                             │
                          │   └───┬───────┬───┘                             │
                          │       │       │ publish refund/notify events    │
                          │       │       ▼                                 │
                          │       │   ┌────────┐   consumer   ┌───────────┐ │
                          │       │   │  SQS   │─────────────▶│ (same svc │ │
                          │       │   └────────┘  + DLQ       │  @Listener│ │
                          │       │                           └─────┬─────┘ │
                          │       ▼                                 ▼       │
                          │   ┌────────────┐                  ┌─────────┐   │
                          │   │ RDS Postgres│                 │   SNS   │──▶ email
                          │   └────────────┘                  └─────────┘   │
                          │                                                 │
                          │   ┌──────────────┐  ┌──────────────┐            │
                          │   │ S3 (receipts)│  │Secrets Manager│           │
                          │   └──────────────┘  └──────────────┘            │
                          │                                                 │
                          │   CloudWatch (logs from JSON appender, metrics  │
                          │   scraped from /actuator/prometheus, alarms)    │
                          └──────────────────────────────────────────────┘
```

## Service-by-service mapping

| AWS service | Replaces / adds | Where it touches the code |
|---|---|---|
| **RDS for PostgreSQL** | Local Docker Postgres | Only `application-prod.properties` JDBC URL changes; Flyway/JPA untouched. DB creds come from Secrets Manager. |
| **Amazon Cognito** | In-memory users + HTTP Basic | New `prod` security filter chain: `oauth2ResourceServer().jwt()` validating Cognito-issued JWTs. Map Cognito groups → `ROLE_USER`/`ROLE_ADMIN`. Keep Basic auth only under the `local` profile. |
| **Amazon SQS** (+ DLQ) | Nothing (new capability) | On refund creation, publish a `RefundRequested` event; a `@SqsListener` processes it (gateway call, status update, notification). Idempotent consumer keyed on refund id. Failed messages → dead-letter queue after N retries. |
| **Amazon SNS** | Nothing (new capability) | Consumer publishes `PaymentCompleted` / `RefundProcessed` notifications; SNS fans out to email (and future webhooks). |
| **Amazon S3** | Nothing (new capability) | Generate a PDF receipt on payment completion (the repo's `pdf` skill), store in S3, expose a pre-signed download URL via the API. |
| **Secrets Manager** | Plaintext `application-local.properties` | DB password, Cognito client secret, Stripe keys. Loaded via `spring-cloud-aws-starter-secrets-manager`. |
| **SSM Parameter Store** | Hard-coded config | Non-secret config (queue URLs, bucket names, SNS topic ARNs). |
| **CloudWatch Logs** | stdout only | Already emitting JSON (logback `!local` profile) — Fargate's awslogs driver ships it; `correlationId` is a queryable field in Logs Insights. |
| **CloudWatch Metrics + Alarms** | `/actuator/prometheus` exposed | Scrape with CloudWatch agent (or ADOT). Alarms on 5xx rate, p99 latency, SQS queue depth, DLQ > 0. |
| **ECS Fargate** | `./mvnw spring-boot:run` | Run the container (built by the Track-4 Dockerfile) behind an ALB. Task role grants SQS/SNS/S3/Secrets access — **no static keys in the container**. |
| **ECR** | — | Registry for the service image; CI pushes here. |

## IAM principle

The service authenticates to AWS via an **ECS task role**, not access keys. Locally,
the AWS SDK falls back to the default credential chain (`aws configure` / SSO) pointed at
LocalStack via an endpoint override. This is the single most important "production" detail
to be able to explain in an interview: **no long-lived secrets in the app or image.**

## Spring profiles

- `local` — Docker Postgres, HTTP Basic, LocalStack endpoints, human-readable logs.
- `prod` — RDS, Cognito JWT, real AWS endpoints (default credential chain), JSON logs.

Profile-specific `SecurityFilterChain` beans keep dev ergonomics without weakening prod.

## Local emulation (LocalStack)

The commented LocalStack block in `docker-compose.yml` covers `cognito-idp, sqs, s3`. Bring
it up, point `spring.cloud.aws.*.endpoint` at `http://localhost:4566`, and the full async +
storage + auth flow runs locally with no AWS account and no cost.

## Suggested build order

1. **SQS + SNS** — async refund/notification flow (highest "real system" signal).
2. **S3** — PDF receipts on completion.
3. **Cognito** — replace Basic auth with JWT.
4. **Secrets Manager / SSM** — externalize config.
5. **RDS + ECS + ECR** — deploy (pairs with the Track-4 CI/CD + Terraform work).

Each step works against LocalStack first, then flips to real AWS by changing endpoints only.

## Cost guardrail

When moving off LocalStack to a real sandbox account: use a dedicated account, set an AWS
Budget (~$5–10) with an alarm, prefer the smallest RDS/Fargate sizes, and tear down with
`terraform destroy` between demo sessions.
