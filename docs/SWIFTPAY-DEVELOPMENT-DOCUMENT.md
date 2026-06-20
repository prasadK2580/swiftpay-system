# SwiftPay — Development Document (NotebookLM / Validation)

**Project name:** SwiftPay — Real-Time Payment Ledger  
**Purpose:** Hackathon submission for a peer-to-peer payment platform with event-driven microservices, ACID settlement, idempotency, and high-throughput load testing.  
**Repository:** `transaction-gateway-service` (Maven multi-module monorepo)  
**Date context:** 2026 — Java 21, Spring Boot 3.4.7

---

## 1. Executive summary

SwiftPay lets clients send money between users (accounts). The system accepts payments quickly over HTTP, validates idempotency and balance, stores a **PENDING** transaction, and settles money **asynchronously** via Kafka. Settlement debits the sender and credits the receiver in one database transaction. The gateway updates final status when settlement completes or fails.

The solution is split into **two deployable microservices** plus a **shared library**:

| Component | Port | Name in requirements |
|-----------|------|---------------------|
| `gateway-service` | 8080 | **Service A** — Transaction Gateway |
| `ledger-service` | 8081 | **Service B** — Ledger |
| `swiftpay-shared` | (library) | Shared Kafka events and API errors |

**Communication between services:**

- **HTTP:** Gateway asks Ledger for authoritative account balance before accepting a payment.
- **Kafka:** Gateway publishes `payment.initiated`; Ledger settles and publishes `payment.completed` or `payment.failed`; Gateway consumes those to update transaction status.

**Infrastructure:** PostgreSQL, Redis, Apache Kafka, Docker Compose, Kubernetes manifests, GitHub Actions CI.

---

## 2. Business problem and goals

### Problem

Build a payment gateway that can:

1. Accept payment requests at high rate (target **250 transactions per second** for **1 million** total requests in load tests).
2. Guarantee **no duplicate processing** (idempotency).
3. Reject or fail payments when the sender has **insufficient funds**.
4. Settle transfers **atomically** (debit + credit in one transaction).
5. Avoid database **deadlocks** when many payments hit the same accounts.
6. Expose APIs, health checks, documentation, and deployment artifacts suitable for a hackathon jury.

### What we built

A working end-to-end payment pipeline with two Spring Boot services, automated tests, Docker deployment, optional Kubernetes, load-test scripts, and documentation mapping every requirement to code.

### What we did not build (bonus only)

- **Service C** — Analytics service writing to ClickHouse (optional hackathon bonus; not implemented).

---

## 3. System architecture

### 3.1 High-level diagram (text)

```
                    ┌─────────────────────────────────────┐
                    │           Client / Load test         │
                    └──────────────────┬──────────────────┘
                                       │ HTTP POST/GET
                                       ▼
┌──────────────────────────────────────────────────────────────────┐
│  SERVICE A — gateway-service (:8080)                              │
│  • POST /v1/payments (Idempotency-Key header)                     │
│  • Redis: idempotency, balance cache, tx dedup (24h TTL)          │
│  • PostgreSQL: transactions table (PENDING → COMPLETED/FAILED)  │
│  • HTTP → Ledger: GET balance before validate                     │
│  • Kafka PRODUCE: payment.initiated                               │
│  • Kafka CONSUME: payment.completed, payment.failed               │
└───────────────┬───────────────────────────────┬──────────────────┘
                │ HTTP                           │ Kafka
                ▼                                ▼
┌───────────────────────────────┐   ┌────────────────────────────┐
│  SERVICE B — ledger (:8081)   │   │  Apache Kafka               │
│  • GET /v1/accounts/{id}/balance│◄──│  payment.initiated          │
│  • GET /v1/history/{userId}    │   │  payment.completed          │
│  • PostgreSQL: accounts +       │──►│  payment.failed             │
│    settlement on transactions  │   └────────────────────────────┘
│  • Kafka CONSUME: initiated     │
│  • Kafka PRODUCE: completed/    │
│    failed                       │
│  • Redis: sync balance cache    │
│    after settlement             │
└───────────────────────────────┘
                │
                ▼
        ┌───────────────┐
        │  PostgreSQL   │  (shared database: accounts + transactions)
        │  Redis        │
        └───────────────┘
```

### 3.2 Why two services

| Concern | Service A (Gateway) | Service B (Ledger) |
|---------|----------------------|-------------------|
| Accept HTTP from clients | Yes | No (internal/reporting APIs only) |
| Fast validation path | Redis + HTTP balance | — |
| Money movement (debit/credit) | No | Yes (ACID) |
| Publish payment initiated | Yes | — |
| Consume and settle | — | Yes |
| Transaction history reporting | — | Yes |

This matches classic **microservice boundaries**: gateway orchestrates; ledger owns the financial truth for accounts.

### 3.3 Shared database note

Both services connect to the same PostgreSQL database `swiftpay` for the hackathon (pragmatic shared DB):

- **Gateway** JPA entity: `transactions` only (`ddl-auto: validate`).
- **Ledger** JPA entities: `accounts` and `transactions` (`ddl-auto: validate`).

**Schema and seed data** (accounts 1001 / 2002):

| Environment | How schema + seed are applied |
|-------------|------------------------------|
| Local IDE (ledger default profile) | Spring `sql.init` runs `schema.sql` / `data.sql` on ledger startup |
| Docker Compose / CI `docker-build` | Postgres mounts scripts into `docker-entrypoint-initdb.d` (first volume only); apps use `prod` profile → `sql.init.mode: never` |
| CI `integration-test` job | Manual `psql` after infra starts (same SQL files) |
| Kubernetes | Apply `schema.sql` / `data.sql` manually after Postgres is ready (see `k8s/README.md`) |

---

## 4. Payment flow (step by step)

### 4.1 Happy path — create payment to COMPLETED

1. **Client** sends `POST /v1/payments` to gateway with header `Idempotency-Key` and JSON body `{ senderId, receiverId, amount, currency }`.

2. **Gateway — idempotency**  
   - Redis key `idempotency:{key}` (24 hour TTL).  
   - If same key was already used with different body → **409 Conflict**.  
   - If replay of successful request → return cached outcome.

3. **Gateway — business validation**  
   - Validate ids, amount, currency rules.

4. **Gateway — balance check**  
   - HTTP call to Ledger: `GET /v1/accounts/{senderId}/balance?currency=INR`.  
   - Ledger reads `accounts` table (source of truth).  
   - Gateway writes balance into Redis `balance:{senderId}`.  
   - Gateway reads Redis and checks `amount <= available`.  
   - If insufficient → **422 Unprocessable Entity** (no row inserted).

5. **Gateway — reserve transaction**  
   - Redis `tx:processed:{transactionId}` prevents duplicate processing (24h).

6. **Gateway — persist**  
   - Insert row in `transactions` with status **PENDING**.  
   - After DB commit, publish **PaymentInitiatedEvent** to Kafka topic `payment.initiated`.

7. **Gateway — HTTP response**  
   - **201 Created** with `transactionId` (server-generated UUID) and `status: PENDING`.

8. **Ledger — consume Kafka**  
   - `PaymentInitiatedKafkaListener` receives event.  
   - `SettlementService` delegates to `PaymentSettlementProcessor` in one `@Transactional` (isolation REPEATABLE_READ).

9. **Ledger — settlement**  
   - Lock transaction row `FOR UPDATE`.  
   - Lock both account rows in **ordered** user id order (`min(userId)` then `max(userId)`) to prevent deadlock.  
   - Re-check balance inside transaction.  
   - If insufficient → set transaction **FAILED**, emit `payment.failed` (no debit).  
   - If OK → debit sender, credit receiver, set transaction **COMPLETED**, emit `payment.completed`.

10. **Gateway — feedback**  
    - `PaymentResultKafkaListener` consumes completed/failed.  
    - Updates `transactions.status` if still PENDING.  
    - Updates Redis status/balance caches.

11. **Verify outcome**  
    - `GET /v1/payments/{transactionId}` on gateway (:8080) until status is **COMPLETED** or **FAILED**.

### 4.2 Insufficient funds — two layers

| Layer | When | Result |
|-------|------|--------|
| Gateway | Before Kafka, Redis balance check after HTTP refresh | HTTP **422** |
| Ledger | During settlement transaction | **FAILED** + `payment.failed` event, no debit |

### 4.3 Idempotency design

| Mechanism | Key | TTL | Purpose |
|-----------|-----|-----|---------|
| Client key | `idempotency:{Idempotency-Key}` | 24h | Same client retry returns same result |
| Server transaction id | `tx:processed:{transactionId}` | 24h | Prevent double processing of same UUID |

**Note:** The hackathon text may mention `transaction_id` for idempotency; this implementation uses header **`Idempotency-Key`** plus server-generated **`transactionId`**.

---

## 5. API reference

### 5.1 Service A — Gateway (port 8080)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/v1/payments` | Create payment. **Required header:** `Idempotency-Key` |
| GET | `/v1/payments/{transactionId}` | Poll payment status (PENDING → COMPLETED/FAILED) |
| GET | `/health` | Health (DB, Redis, Kafka) |
| GET | `/swagger-ui.html` | OpenAPI UI |

**POST body example:**

```json
{
  "senderId": 1001,
  "receiverId": 2002,
  "amount": 1,
  "currency": "INR"
}
```

**Success response (201):**

```json
{
  "transactionId": "uuid-here",
  "status": "PENDING",
  "senderId": 1001,
  "receiverId": 2002,
  "amount": 1,
  "currency": "INR"
}
```

**Common HTTP status codes:**

| Code | Meaning |
|------|---------|
| 201 | Accepted, PENDING |
| 400 | Validation error |
| 409 | Idempotency conflict |
| 422 | Insufficient funds |
| 404 | Transaction not found (GET) |

### 5.2 Service B — Ledger (port 8081)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/v1/accounts/{userId}/balance?currency=INR` | Authoritative balance (gateway calls this) |
| GET | `/v1/history/{userId}?limit=50` | Transaction history (paginated, max 200) |
| GET | `/health` | Health |
| GET | `/swagger-ui.html` | OpenAPI UI |

### 5.3 Seed data

Accounts **1001** (10,000 INR) and **2002** (5,000 INR) come from `ledger-service/src/main/resources/data.sql`:

| Environment | When seed runs |
|-------------|----------------|
| Local ledger (default profile) | Ledger startup via Spring `sql.init` |
| `docker compose up` | Postgres first-init (`docker-entrypoint-initdb.d`) |
| CI integration job | Explicit `psql` before tests |

For **1 million** load tests with `amount=1`, run `scripts/sql/ensure-load-test-balance.sql` to set sender balance to 2,000,000,000.

---

## 6. Kafka events

| Topic | Producer | Consumer | Payload type |
|-------|----------|----------|--------------|
| `payment.initiated` | Gateway | Ledger | `PaymentInitiatedEvent` |
| `payment.completed` | Ledger | Gateway | `PaymentCompletedEvent` |
| `payment.failed` | Ledger | Gateway | `PaymentFailedEvent` |

Event classes live in **`swiftpay-shared`** module (`com.swiftpay.shared.event`).

**Consumer retry:** Exponential backoff on transient failures (`KafkaConsumerRetryConfig` in both services).

**Consumer groups:**

- Gateway: `transaction-gateway` (completed/failed)
- Ledger: `ledger-service` (initiated)

---

## 7. Redis keys

| Key pattern | TTL | Service | Purpose |
|-------------|-----|---------|---------|
| `idempotency:{key}` | 24h | Gateway | Idempotency lock / cached response |
| `tx:processed:{transactionId}` | 24h | Gateway | Dedup server transaction id |
| `balance:{userId}` | 24h | Gateway (+ Ledger sync) | Fast balance read on hot path |
| `tx:status:{transactionId}` | 24h | Gateway | Cached final status |

---

## 8. Database schema

### Table: accounts (ledger-owned)

| Column | Type | Notes |
|--------|------|-------|
| user_id | BIGINT PK | Account id |
| balance | DOUBLE | Current balance |
| currency | VARCHAR(3) | e.g. INR |

### Table: transactions (gateway creates; ledger updates status)

| Column | Type | Notes |
|--------|------|-------|
| transaction_id | VARCHAR PK | UUID from gateway |
| idempotency_key | VARCHAR UNIQUE | Client key |
| sender_id | BIGINT | |
| receiver_id | BIGINT | |
| amount | DOUBLE | |
| currency | VARCHAR(3) | |
| status | VARCHAR | PENDING, COMPLETED, FAILED |
| created_at | TIMESTAMP | |

---

## 9. Concurrency and deadlock prevention

**Problem:** Many concurrent payments between users 1001 and 2002 could lock account rows in different orders and cause PostgreSQL deadlocks.

**Solution:** `LedgerLockOrdering` in shared module — always lock accounts in order `min(userId)` then `max(userId)` before debit/credit.

**Under load:** Expect **queueing** on hot accounts (latency), not deadlocks.

---

## 10. Code structure and design patterns

### 10.1 Maven modules

```
transaction-gateway-service/
├── pom.xml                    # Parent POM
├── swiftpay-shared/           # Shared jar (no Spring Boot app)
├── gateway-service/           # Service A executable
└── ledger-service/            # Service B executable
```

### 10.2 Layered architecture (per service)

**Gateway (Service A)** — concrete classes by package; open the class to see Redis/Kafka/HTTP code:

| Package | Responsibility | Key classes |
|---------|----------------|-------------|
| `controller` | REST + DTOs | `PaymentController` |
| `service` | Orchestration | `PaymentService`, `PendingPaymentService`, `PaymentIdempotencyService` |
| `service.validation` | Rules + balance check | `PaymentRequestValidator` |
| `repository` | PostgreSQL | `PaymentRepository`, `TransactionJpaRepository` |
| `cache` | Redis | `PaymentIdempotencyCache`, `PaymentBalanceCache`, `DuplicatePaymentChecker`, `PaymentStatusCache` |
| `client` | HTTP to ledger | `LedgerBalanceClient` |
| `event` | Kafka publish | `PaymentEventProducer` |
| `infrastructure.kafka` | Kafka consume | `PaymentResultKafkaListener` |
| `config` | Spring `@Configuration` | Kafka, Redis, HTTP |

**Ledger (Service B)** — same layered style; Postgres/Redis/Kafka adapters under `infrastructure/`:

| Package | Responsibility | Key classes |
|---------|----------------|-------------|
| `controller` | REST + DTOs | `LedgerBalanceController`, `LedgerHistoryController` |
| `service` | Settlement + queries | `SettlementService`, `LedgerService`, `PaymentSettlementProcessor`, `PaymentInitiatedMessageValidator` |
| `service.impl` | Default implementations | `SettlementServiceImpl`, `LedgerServiceImpl` |
| `repository` | Domain + JPA | `AccountRepository`, `SettlementTransactionRepository`, `PaymentHistoryRepository`, `*JpaRepository` |
| `infrastructure.persistence` | Postgres adapters | `AccountPostgresRepository`, `SettlementTransactionPostgresRepository`, `PaymentHistoryPostgresRepository` |
| `cache` | Redis balance | `AccountBalanceCache` |
| `infrastructure.redis` | Redis adapter | `RedisAccountBalanceCache` |
| `event` | Kafka publish interface | `SettlementEventProducer` |
| `infrastructure.kafka` | Kafka impl + consume | `KafkaSettlementEventProducer`, `PaymentInitiatedKafkaListener` |
| `config` | Spring `@Configuration` | Kafka, Redis, OpenAPI |

### 10.3 Design highlights

- **Single responsibility:** Kafka listeners delegate to services; validation separate from persistence.
- **Flat packages:** Gateway and ledger use `cache/`, `client/`, `event/`, `repository/` (gateway) or `repository/` + `infrastructure/*` (ledger) — no hexagonal `port/` interfaces.
- **Async settlement:** Gateway saves PENDING, publishes Kafka; ledger settles; gateway updates status on feedback.

### 10.4 Key classes (quick index)

| Class | Module | Role |
|-------|--------|------|
| `PaymentController` | gateway | REST entry for payments |
| `PaymentService` | gateway | Create payment + get status |
| `PendingPaymentService` | gateway | Save PENDING, afterCommit → Redis + Kafka |
| `LedgerBalanceClient` | gateway | HTTP client to ledger balance API |
| `PaymentIdempotencyCache` | gateway | 24h idempotency (Redis) |
| `PaymentEventProducer` | gateway | Publish `payment.initiated` |
| `PaymentResultKafkaListener` | gateway | Consume completed/failed |
| `LedgerBalanceController` | ledger | Balance HTTP API |
| `LedgerHistoryController` | ledger | History HTTP API |
| `PaymentInitiatedKafkaListener` | ledger | Consume initiated |
| `PaymentSettlementProcessor` | ledger | Atomic settlement |
| `SettlementEventProducer` | ledger | Publish completed/failed |
| `GlobalExceptionHandler` | shared | Uniform JSON errors |

---

## 11. Deployment and operations

### 11.1 Docker Compose (full stack)

```bash
docker compose up --build
```

Services: `postgres`, `redis`, `kafka`, `ledger` (8081), `gateway` (8080).

- **Postgres init:** `schema.sql` and `data.sql` mounted to `/docker-entrypoint-initdb.d/` (runs once per new volume; use `docker compose down -v` to reset).
- **Profiles:** `SPRING_PROFILES_ACTIVE=docker,prod,performance` on ledger and gateway (`prod` disables Spring SQL init; Hibernate `validate` expects tables from Postgres init).
- **Gateway → ledger:** `APP_LEDGER_HTTP_BASE_URL=http://ledger:8081`.

### 11.2 Local development (two terminals)

```powershell
docker compose up -d postgres redis kafka
.\scripts\run-ledger.ps1    # port 8081 first
.\scripts\run-gateway.ps1   # port 8080
```

### 11.3 Kubernetes

Manifests under `k8s/`: namespace, postgres, redis, kafka, separate deployments for `swiftpay-gateway` and `swiftpay-ledger`.

### 11.4 CI/CD

GitHub Actions (`.github/workflows/ci.yml`):

| Job | Steps |
|-----|--------|
| **compile-and-unit-test** | `./mvnw clean install -DskipTests`; `./mvnw test -pl gateway-service -Dtest=PaymentRequestValidatorTest` (parent POM excludes `*IntegrationTest`; ledger has no unit tests) |
| **integration-test** | `docker compose up -d postgres redis kafka` → wait for health → `psql` seed `schema.sql` + `data.sql` → `./mvnw test -pl ledger-service,gateway-service -Dtest='*IntegrationTest' -Dspring.profiles.active=integration-test` |
| **docker-build** | `docker compose up -d --build` → curl gateway and ledger `/health` (Postgres init via compose mounts) |

Each job tears down with `docker compose down -v` so the next run gets a fresh Postgres volume.

---

## 12. Testing

| Test type | Location | What it proves |
|-----------|----------|----------------|
| Unit | `PaymentRequestValidatorTest` | Validation rules (only unit test run in CI) |
| Ledger integration | `LedgerBalanceApiIntegrationTest` | Balance API 200/404 |
| Gateway integration | `PaymentApiIntegrationTest` | POST returns 201 PENDING |
| Gateway integration | `InsufficientFundsIntegrationTest` | POST returns 422 |
| Gateway E2E | `PaymentStatusIntegrationTest` | POST + GET `/v1/payments/{id}` until COMPLETED |
| Gateway integration | `UnknownReceiverIntegrationTest` | Unknown account → 404 |

**Gateway E2E harness:** `IntegrationTestBase` starts `LedgerApplication` in-process on a random port before `GatewayApplication` loads, sets `app.ledger.http.base-url`, uses unique Kafka consumer group IDs, and polls `GET /v1/accounts/1001/balance` until seed data is ready. Each test method runs `@Sql("/sql/integration-test-reset.sql")` to reset accounts and transactions.

**Run locally (unit only — same as CI):**

```bash
./mvnw -B clean install -DskipTests
./mvnw -B test -pl gateway-service -Dtest=PaymentRequestValidatorTest
```

**Run integration tests (same as CI):**

```bash
docker compose up -d postgres redis kafka
# seed DB (see DEPLOYMENT-GUIDE.md), then:
./mvnw -B clean install -DskipTests
./mvnw -B test -pl ledger-service,gateway-service -Dtest='*IntegrationTest' -Dspring.profiles.active=integration-test
```

---

## 13. Load testing (250 TPS × 1 million)

### 13.1 Requirement

- **250 TPS** sustained arrival rate
- **1,000,000** HTTP POST requests to gateway
- **PCAP** network capture as evidence
- Document bottlenecks and balance impact

Expected duration: ~66.7 minutes at full 250 TPS.

### 13.2 How to run

1. Top up balance: `scripts/sql/ensure-load-test-balance.sql`
2. Start stack: `docker compose up --build`
3. Run (Admin PowerShell for PCAP on Windows):

```powershell
.\scripts\run-load-test-1m-with-pcap.ps1
```

Or without PCAP:

```powershell
node scripts/load-test.mjs --tps 250 --total 1000000
```

Load test targets **gateway only** (`http://localhost:8080/v1/payments`); each payment also triggers ledger balance HTTP internally.

### 13.3 Evidence artifacts

Store under `evidence/`:

- `load-test-*.log`
- `load-test-*-summary.json` (TPS, status counts, latency percentiles)
- `load-test-*.pcapng` (for submission)

### 13.4 Known results (from development)

| Run | Outcome |
|-----|---------|
| 10k @ 250 TPS target | ~10,000 × HTTP 201, ~200 achieved TPS, 0 errors (with balance topped up) |
| 1M @ 250 TPS (early) | Stalled with errors when balance low and system overloaded |
| Full 1M | Must be executed on your machine; fill `docs/LOAD-TEST-EVIDENCE.md` |

**Important:** HTTP `201 PENDING` can reach 1M while Kafka settlement **lags**; many transactions may stay PENDING during heavy load. That is expected for async architecture.

---

## 14. Hackathon requirements mapping

### Mandatory functional

| Requirement | Status | Implementation |
|-------------|--------|----------------|
| POST /v1/payments | Done | `PaymentController` |
| 24h idempotency (Redis) | Done | `PaymentIdempotencyCache`, `DuplicatePaymentChecker` |
| Balance validation | Done | HTTP balance + Redis + `PaymentRequestValidator` |
| Save PENDING + publish initiated | Done | `PendingPaymentService`, `PaymentEventProducer` |
| Consume PaymentInitiated | Done | `PaymentInitiatedKafkaListener` |
| Atomic debit/credit | Done | `PaymentSettlementProcessor` |
| PaymentCompleted / Failed events | Done | `SettlementEventProducer` |
| GET payment status | Done | `GET /v1/payments/{transactionId}` — `PaymentService` |
| GET transaction history | Done | `LedgerHistoryController` with `?limit=` (reporting) |

### Mandatory non-functional

| Requirement | Status | Implementation |
|-------------|--------|----------------|
| Swagger / OpenAPI | Done | springdoc on both services |
| HTTP errors with body | Done | `GlobalExceptionHandler` |
| Kafka consumer retry | Done | `KafkaConsumerRetryConfig` |
| /health | Done | Spring Actuator `/health` |
| Dockerfile | Done | Per-service Dockerfiles |
| docker-compose | Done | Full stack |
| Kubernetes | Done | `k8s/` |
| GitHub Actions CI | Done | `.github/workflows/ci.yml` |
| Load test 250 TPS / 1M | Scripts ready | `scripts/load-test.mjs`, PCAP script, evidence doc |

### Bonus

| Item | Status |
|------|--------|
| Service C / ClickHouse analytics | Not implemented |

---

## 15. Five-minute demo script (for judges)

1. Run `docker compose up --build`.
2. Open http://localhost:8080/swagger-ui.html
3. POST payment: sender `1001`, receiver `2002`, amount `1`, header `Idempotency-Key: demo-001`.
4. Copy `transactionId`; GET `/v1/payments/{transactionId}` until **COMPLETED** (only status endpoint clients need).
5. POST payment with amount `999999999` → expect **422** insufficient funds.
6. (Optional) Ledger history: GET `/v1/history/1001?limit=10` on :8081 for reporting.
7. (Optional) Show logs: `[PAYMENT_FLOW]`, `[LEDGER_FLOW]`, Kafka emit/receive lines.

---

## 16. Configuration reference

| Property | Default | Meaning |
|----------|---------|---------|
| `app.ledger.http.base-url` | `http://localhost:8081` | Ledger URL for gateway |
| `app.gateway.ledger-balance.source` | `http` | Balance via HTTP (not direct DB) |
| `app.redis.ttl-hours` | 24 | Redis key TTL |
| `app.kafka.topics.payment-initiated` | `payment.initiated` | Topic names |
| `server.port` | 8080 / 8081 | Gateway / Ledger |

Docker profile: `application-docker.yml` in each service.  
Load profile: `application-performance.yml` (Hikari pool, Tomcat threads, Kafka concurrency).

---

## 17. Glossary

| Term | Definition |
|------|------------|
| **Service A / Gateway** | Accepts client payments, idempotency, fast validation |
| **Service B / Ledger** | Settles money, owns balance truth, history API |
| **PENDING** | Payment saved, settlement not finished |
| **COMPLETED** | Debit/credit succeeded |
| **FAILED** | Settlement failed (e.g. insufficient funds at ledger) |
| **Settlement** | Atomic update of accounts + transaction status |
| **Idempotency-Key** | Client header for safe retries |
| **transactionId** | Server-generated UUID per payment |

---

## 18. Related documents in this repository

| File | Content |
|------|---------|
| `README.md` | Quick start |
| `docs/CODEBASE-GUIDE.md` | Package map, flowcharts |
| `docs/DEPLOYMENT-GUIDE.md` | Docker, K8s, balances |
| `docs/HACKATHON-CHECKLIST.md` | Requirement checklist |
| `docs/LOAD-TEST-EVIDENCE.md` | Load test procedure and results template |
| `k8s/README.md` | Kubernetes deploy steps |

---

## 19. Validation questions (for NotebookLM)

Use these prompts when validating the document:

1. What are the two microservices and how do they communicate?
2. Walk through the payment flow from POST to COMPLETED.
3. How is idempotency implemented and what are the Redis keys?
4. How does the system prevent deadlocks on concurrent transfers?
5. Where is insufficient funds checked (gateway vs ledger)?
6. List all REST APIs and which service exposes them.
7. What Kafka topics exist and who produces/consumes each?
8. How do you run the system locally and with Docker?
9. What is required for the 1 million transaction load test?
10. Which hackathon requirements are met and which bonus items are missing?

---

*End of SwiftPay Development Document*
