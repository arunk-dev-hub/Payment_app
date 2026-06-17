# Enhancement Tracker — Payment Service

Living checklist of every proposed enhancement. Update the **Status** and **Done** date as
work lands. Legend: ✅ done · 🔄 in progress · ⬜ planned.

Last updated: **2026-06-16**

---

## Track 1 — API maturity  ✅ COMPLETE

| # | Enhancement | Status | Done | Notes / key files |
|---|---|---|---|---|
| 1.1 | Correlation / request IDs (MDC → logs, response header, error body) | ✅ | 2026-06-16 | `CorrelationIdFilter`, `ErrorResponse.correlationId`, `GlobalExceptionHandler` |
| 1.2 | Structured JSON logging (readable local / JSON prod) | ✅ | 2026-06-16 | `logback-spring.xml`, logstash-logback-encoder |
| 1.3 | Optimistic locking (`@Version`, 409 on conflict) | ✅ | 2026-06-16 | `Payment`, `Refund`, `V2__add_optimistic_locking.sql` |
| 1.4 | Rate limiting (Bucket4j, 20 writes/min/caller, 429) | ✅ | 2026-06-16 | `RateLimitingInterceptor`, `WebConfig`, `RateLimitExceededException` |
| 1.5 | OpenAPI 3 / Swagger UI | ✅ | 2026-06-16 | `OpenApiConfig`, springdoc 3.0.3, `SecurityConfig` permits |
| 1.6 | Prometheus metrics endpoint | ✅ | 2026-06-16 | `application.properties`, micrometer-registry-prometheus |

**Verification:** `./mvnw test` → 15/15 passing, BUILD SUCCESS (Testcontainers + V1/V2 Flyway).

---

## Track 2 — Real payment flow (Stripe)  ✅ COMPLETE

| # | Enhancement | Status | Done | Notes |
|---|---|---|---|---|
| 2.1 | Stripe SDK + key management | ✅ | 2026-06-17 | Keys via properties / env fallback |
| 2.2 | Create PaymentIntent on payment create | ✅ | 2026-06-17 | Store intent id & client secret on `Payment` |
| 2.3 | Signature-verified `POST /webhooks/stripe` | ✅ | 2026-06-17 | CSRF-exempt, permitAll webhook endpoint |
| 2.4 | Webhook-driven status transitions | ✅ | 2026-06-17 | Automates transitions (PENDING→PROCESSING→COMPLETED/FAILED) |
| 2.5 | Idempotent webhook handling | ✅ | 2026-06-17 | Dedupe via `processed_stripe_events` DB table |
| 2.6 | Tests with Stripe test cards + CLI | ✅ | 2026-06-17 | JUnit 5 unit & MockMvc controller tests |

---

## Track 3 — Cloud integration (LocalStack → AWS)  ⬜ PLANNED

| # | Enhancement | Status | Done | Notes |
|---|---|---|---|---|
| 3.1 | SQS async refund pipeline (+ DLQ) | ⬜ | | `@SqsListener`, idempotent consumer |
| 3.2 | SNS notifications (email/webhook fan-out) | ⬜ | | Published by the consumer |
| 3.3 | S3 PDF receipts + pre-signed URLs | ⬜ | | Uses repo `pdf` skill |
| 3.4 | Cognito JWT auth (replace Basic) | ⬜ | | `oauth2ResourceServer().jwt()`, groups→roles |
| 3.5 | Secrets Manager / SSM config | ⬜ | | DB creds, Stripe keys, ARNs |
| 3.6 | RDS PostgreSQL (`prod` profile) | ⬜ | | Config-only change |
| 3.7 | LocalStack wiring in docker-compose | ⬜ | | Uncomment stub, endpoint overrides |

---

## Track 4 — DevOps / CI-CD  ⬜ PLANNED

| # | Enhancement | Status | Done | Notes |
|---|---|---|---|---|
| 4.1 | Multi-stage Dockerfile | ⬜ | | Maven build → slim JRE runtime |
| 4.2 | GitHub Actions: build → test → push ECR | ⬜ | | Testcontainers in CI |
| 4.3 | Deploy to ECS Fargate (task role, no keys) | ⬜ | | Rolling deploy on health probes |
| 4.4 | Terraform IaC (VPC, RDS, ECS, ALB, SQS, SNS, S3, Cognito, IAM) | ⬜ | | Biggest IaC signal |
| 4.5 | CloudWatch dashboards + alarms | ⬜ | | 5xx, p99, queue depth, DLQ>0 |

---

## Backlog / nice-to-have (not yet scheduled)

| Idea | Why |
|---|---|
| Refresh-token / API-key auth for service-to-service | More realistic auth story |
| Audit log table (who changed what, when) | Compliance signal for payments |
| Distributed rate limiting via Redis | Multi-instance correctness |
| OpenTelemetry tracing (ADOT → X-Ray) | Full distributed tracing |
| Contract tests / Pact | API consumer safety |
| Load test (k6 / Gatling) results in README | Evidence of performance work |
