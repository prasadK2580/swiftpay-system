# Hackathon requirement checklist

## Mandatory functional

| Requirement | Implementation | Evidence |
|-------------|----------------|----------|
| POST /v1/payments | `PaymentController` | Swagger, integration tests |
| Idempotency 24h (Redis) | `PaymentIdempotencyCache`, `DuplicatePaymentChecker` | README, Redis keys |
| Balance validation | HTTP `GET /v1/accounts/{id}/balance` → Redis → `PaymentRequestValidator` | `InsufficientFundsIntegrationTest`, `LedgerBalanceApiIntegrationTest` |
| Save PENDING + PaymentInitiated | `PendingPaymentService` + `PaymentEventProducer` | Kafka topic `payment.initiated` |
| Consume PaymentInitiated | `PaymentInitiatedKafkaListener` | Integration tests + Kafka logs |
| Atomic debit/credit | `PaymentSettlementProcessor` + ordered locks | CODEBASE-GUIDE |
| PaymentCompleted / Failed | `SettlementEventProducer` | `PaymentStatusIntegrationTest` → GET until COMPLETED |
| GET payment status | `GET /v1/payments/{transactionId}` | `PaymentService.getPaymentStatus`, poll after POST |
| GET transaction history | `GET /v1/history/{userId}?limit=` | Paginated (max 200) |

## Mandatory non-functional

| Requirement | Implementation |
|-------------|----------------|
| Swagger/OpenAPI | springdoc — :8080 and :8081 `/swagger-ui.html` |
| HTTP status + error body | `GlobalExceptionHandler` |
| Kafka consumer retry | `KafkaConsumerRetryConfig` |
| /health | Spring Actuator (`/health`, `/actuator/health`) |
| Dockerfile | `gateway-service/Dockerfile`, `ledger-service/Dockerfile` |
| docker-compose (full stack) | `docker-compose.yml` (gateway + ledger) |
| Kubernetes | `k8s/` (separate deployments) |
| Two microservices | `gateway-service` (A), `ledger-service` (B) |
| GitHub Actions | `.github/workflows/ci.yml` |
| Load test 250 TPS / 1M | `scripts/load-test.mjs` + `docs/LOAD-TEST-EVIDENCE.md` |

## Bonus (not implemented)

| Item | Status |
|------|--------|
| Service C Analytics / ClickHouse | Not started |

## Demo script (5 min)

1. `docker compose up --build`
2. Open Swagger → POST payment `1001` → `2002`, amount `1`
3. GET `http://localhost:8080/v1/payments/{transactionId}` — poll until `COMPLETED`
4. POST with huge amount → `422` insufficient funds
