# Agent Instructions — Payment Service

> Guidance for AI coding agents working in this repo. Keep this file updated as the project evolves.

## What this project is

A **Spring Boot 4 (Java 21) payment REST API** with a **React 19 (Vite) admin frontend**. It manages payments and their refunds, backed by **PostgreSQL** with **Flyway**-managed schema. Auth is HTTP Basic with in-memory users (dev only).

## Tech stack

**Backend**
- Java 21, Spring Boot 4.0.6 (web-mvc, data-jpa, security, validation, actuator)
- PostgreSQL (runtime) + Flyway migrations
- Lombok (annotation processing; excluded from the boot jar)
- Testcontainers + JUnit 5 for tests
- Build: Maven (use the wrapper `./mvnw`)

**Frontend** (`frontend/`)
- React 19, React Router 7, Vite 5, Axios, react-toastify
- ESLint flat config

## Project layout

```
src/main/java/com/payment/paymentservice/
  PaymentServiceApplication.java
  config/        SecurityConfig.java        # CORS, HTTP Basic, role-based authz, in-memory users
  controller/    PaymentController, RefundController
  service/       *Service (interface) + *ServiceImpl
  repository/    Spring Data JPA repositories
  model/         Payment, Refund (JPA entities; status enums live here)
  dto/           request/response records
  exception/     GlobalExceptionHandler + domain exceptions + ErrorResponse
src/main/resources/
  application.properties           # shared config; activates 'local' profile
  application-local.properties     # Postgres + Flyway for local dev
  db/migration/V1__init_schema.sql # Flyway — schema is owned here, NOT Hibernate
frontend/src/
  api/ pages/ components/ context/ styles/
docker-compose.yml                 # postgres:16-alpine on :5432
```

## Running locally

1. **Start Postgres**: `docker compose up -d` (db `paymentdb`, user/pass `payment`/`payment`, port 5432)
2. **Backend**: `./mvnw spring-boot:run` (serves on :8080, `local` profile by default)
3. **Frontend**: `cd frontend && npm install && npm run dev` (Vite on :5173, proxies `/api` and `/actuator` to :8080)

## Build & test

- Build: `./mvnw clean package`
- Backend tests: `./mvnw test` (uses Testcontainers — Docker must be running)
- Frontend lint: `cd frontend && npm run lint`
- Frontend build: `cd frontend && npm run build`

## API surface

Base path `/api/v1/payments`:
- `POST   /api/v1/payments`               — create payment (optional `Idempotency-Key` header); roles USER, ADMIN
- `GET    /api/v1/payments`               — list/filter (`status`, `currency`, `paymentMethod`, paged); role ADMIN
- `GET    /api/v1/payments/{id}`          — get one; roles USER, ADMIN
- `PATCH  /api/v1/payments/{id}/status`   — update status; role ADMIN
- `POST   /api/v1/payments/{id}/refunds`  — create refund; roles USER, ADMIN
- `GET    /api/v1/payments/{id}/refunds`  — list refunds; roles USER, ADMIN

Non-API endpoints:
- `GET /swagger-ui.html` + `GET /v3/api-docs` — OpenAPI 3 docs (springdoc; permitAll in dev)
- `GET /actuator/{health,info,metrics,prometheus}` — health/info/prometheus permitAll; rest authorized

Statuses: `PaymentStatus` = PENDING, COMPLETED, FAILED · `RefundStatus` = COMPLETED, FAILED.
Dev users (in-memory): `user`/`password` (USER), `admin`/`admin123` (ADMIN).
A Postman collection is at `Payment-Service.postman_collection.json`.

## Conventions & gotchas

- **Schema is owned by Flyway**, not Hibernate. `spring.jpa.hibernate.ddl-auto=validate` — never switch to `update`/`create`. Schema changes go in a new `db/migration/V{n}__*.sql` file; never edit an applied migration.
- **Controllers stay thin**: validation via `@Valid` on DTOs, business logic in `*ServiceImpl`, persistence in repositories.
- **Errors** are centralized in `GlobalExceptionHandler` returning `ErrorResponse`. Reuse the existing domain exceptions (`PaymentNotFoundException`, `InvalidRefundException`, `InvalidPaymentStatusTransitionException`, `IdempotencyConflictException`).
- **Idempotency**: payment creation dedupes on the `Idempotency-Key` header (unique constraint on `payments.idempotency_key`).
- **Lombok** generates boilerplate (`@Data`, `@Builder`, `@RequiredArgsConstructor` for constructor injection). Don't hand-write getters/setters/constructors it already provides.
- **CORS** allowlist (in `SecurityConfig`) is `localhost:5173` and `localhost:3000`. Update it there if the frontend origin changes.
- **`open-in-view=false`** — make sure entities/data needed for serialization are loaded inside the service/transaction.
- **Security note**: in-memory users, `csrf` disabled, and Basic auth are dev-only — replace before any real deployment.
- **Correlation IDs**: every request gets an `X-Correlation-Id` (honoured if inbound, else generated) via `CorrelationIdFilter`; it's in the MDC, response header, and `ErrorResponse`. Logs include it (logback-spring.xml: readable under `local`, JSON otherwise).
- **Optimistic locking**: `Payment`/`Refund` carry `@Version`; concurrent edits surface as `409` via `OptimisticLockingFailureException`.
- **Rate limiting**: `RateLimitingInterceptor` (Bucket4j) throttles POST creates to 20/min per caller → `429 RateLimitExceededException`. In-memory; move to Redis for multi-instance.
- **AWS plan**: target cloud architecture is documented in `docs/aws-architecture.md` (design-only, not provisioned).

## Housekeeping

- `*.log`, `.DS_Store`, and `target/` are gitignored — don't force-add them.
- `docker-compose.yml` has a commented LocalStack block reserved for a future slice (Cognito/SQS/S3); leave it unless that work is in scope.

## Installed agent skills (`.claude/skills/`)

Sourced from [anthropics/skills](https://github.com/anthropics/skills). Invoke with `/<skill-name>`.
- **skill-creator** — build/optimize custom skills for this repo.
- **webapp-testing** — Playwright-driven testing of the React frontend against the running backend. Requires Python `playwright` (`pip install playwright && playwright install chromium`).
- **frontend-design** — visual/UX design guidance for the React admin UI.
- **pdf** — read/extract/create/fill PDFs, e.g. payment receipts & invoices. Core needs `pypdf pdfplumber reportlab`; OCR adds `pytesseract pdf2image`.
- **xlsx** — read/create/edit spreadsheets, e.g. settlement/reconciliation reports. Requires `openpyxl pandas` (formula recalc needs LibreOffice/`soffice`).
