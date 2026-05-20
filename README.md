# SwiftPay — Real-Time Payment Ledger

Hackathon submission: event-driven P2P transfers with PostgreSQL, Kafka, Redis, Docker, Kubernetes, and CI/CD.

## Maven modules

| Module | Port | Role |
|--------|------|------|
| `swiftpay-shared` | — | Kafka events, exceptions, shared domain enums |
| `gateway-service` | 8080 | Service A — payments API, idempotency, Redis balance cache |
| `ledger-service` | 8081 | Service B — settlement, balance/history API, account DB |

```
Client → gateway (8080) → PostgreSQL transactions (PENDING)
              ↓ Redis (idempotency, balance cache)
              ↓ HTTP balance check → ledger (8081)
              ↓ Kafka payment.initiated
         ledger → atomic debit/credit on accounts
              ↓ Kafka payment.completed | payment.failed
         gateway → update status + Redis
```

## Architecture

Logical **Service A** (gateway) and **Service B** (ledger) are separate Spring Boot apps in one repo, connected by **Kafka** (settlement) and **HTTP** (`APP_LEDGER_HTTP_BASE_URL`, default `http://ledger:8081` in Docker).

## Mandatory stack

| Technology | Usage |
|------------|--------|
| Java 21 | Spring Boot 3.4 |
| PostgreSQL | `transactions` (gateway), `accounts` + shared reads (ledger) |
| Apache Kafka | `payment.initiated`, `payment.completed`, `payment.failed` |
| Redis | 24h idempotency, balance cache, tx dedup |
| OpenAPI | Gateway: :8080/swagger-ui.html · Ledger: :8081/swagger-ui.html |
| Docker | `gateway-service/Dockerfile`, `ledger-service/Dockerfile`, `docker-compose.yml` |
| Kubernetes | `k8s/` manifests |
| GitHub Actions | `.github/workflows/ci.yml` |

## Quick start (Docker Compose)

```bash
docker compose up --build
```

| URL | Purpose |
|-----|---------|
| http://localhost:8080/swagger-ui.html | Gateway API (payments) |
| http://localhost:8080/health | Gateway health |
| http://localhost:8081/health | Ledger health |
| http://localhost:8080/v1/payments | POST payment (`Idempotency-Key`) |
| http://localhost:8080/v1/payments/{transactionId} | GET payment status (poll until COMPLETED) |
| http://localhost:8081/v1/accounts/1001/balance?currency=INR | Authoritative balance |
| http://localhost:8081/v1/history/1001?limit=50 | Transaction history |

### Sample payment

```bash
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: demo-key-001" \
  -d '{"senderId":1001,"receiverId":2002,"amount":1,"currency":"INR"}'

curl "http://localhost:8080/v1/payments/{transactionId}"
```

## Local development

```bash
docker compose up -d postgres redis kafka
./mvnw -B clean verify
```

Run from repo root (builds `swiftpay-shared` automatically):

```powershell
docker compose up -d postgres redis kafka

# Terminal 1 — ledger first
.\scripts\run-ledger.ps1

# Terminal 2 — gateway (needs ledger :8081)
.\scripts\run-gateway.ps1
```

Or manually (must build `swiftpay-shared` first):

```powershell
.\mvnw.cmd -pl gateway-service -am install "-Dmaven.test.skip=true"
.\mvnw.cmd -pl gateway-service spring-boot:run "-Dmaven.test.skip=true"
```

Or IDE: `LedgerApplication` (8081), then `GatewayApplication` (8080). Requires `docker compose up -d postgres redis kafka`.

## Production configuration (no hardcoded localhost)

| Environment | Profile | Infra hosts |
|-------------|---------|-------------|
| Local IDE | *(default)* | `localhost` (Postgres, Redis, Kafka, ledger `:8081`) |
| Docker / K8s | `docker,prod` | `postgres`, `redis`, `kafka:29092`, `ledger` / `swiftpay-ledger` |

Override via environment variables (see [`.env.example`](.env.example)):

- `SPRING_DATASOURCE_URL`, `SPRING_KAFKA_BOOTSTRAP_SERVERS`, `SPRING_DATA_REDIS_HOST`
- `APP_LEDGER_HTTP_BASE_URL` — gateway → ledger (required in code if unset)
- `APP_OPENAPI_SERVER_URL` — optional public URL for Swagger (default `/` in prod profile)

`docker compose up` and `k8s/app.yaml` already set these for container networking.

## Kubernetes (Minikube)

```bash
minikube start
eval $(minikube docker-env)
docker build -f ledger-service/Dockerfile -t swiftpay-ledger:local .
docker build -f gateway-service/Dockerfile -t swiftpay-gateway:local .
kubectl apply -f k8s/
kubectl wait --for=condition=ready pod -l app=swiftpay-gateway -n swiftpay --timeout=300s
minikube service swiftpay-gateway -n swiftpay
```

See [docs/DEPLOYMENT-GUIDE.md](docs/DEPLOYMENT-GUIDE.md) and [k8s/README.md](k8s/README.md).

## Tests & CI

```bash
./mvnw test -Dtest='!*IntegrationTest'
docker compose up -d postgres redis kafka
./mvnw test -Pintegration-tests -Dtest='*IntegrationTest' -Dspring.profiles.active=integration-test
```

Cross-service E2E tests live in `gateway-service` (in-process ledger + gateway). Ledger-only tests are in `ledger-service`.

## Documentation

- **[docs/SWIFTPAY-DEVELOPMENT-DOCUMENT.md](docs/SWIFTPAY-DEVELOPMENT-DOCUMENT.md)** — full project summary (ideal for NotebookLM / jury validation)
- [docs/CODEBASE-GUIDE.md](docs/CODEBASE-GUIDE.md)
- [docs/DEPLOYMENT-GUIDE.md](docs/DEPLOYMENT-GUIDE.md)
- [docs/HACKATHON-CHECKLIST.md](docs/HACKATHON-CHECKLIST.md)

## License

Hackathon / educational use.
