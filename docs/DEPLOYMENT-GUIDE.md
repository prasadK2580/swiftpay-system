# SwiftPay — Deployment & CI/CD (step-by-step)

This guide covers **containerization**, **one-command orchestration**, **CI/CD**, and **account balances** in the full stack.

---

## What you have now

| Piece | File / command |
|--------|----------------|
| Infra + app in Docker | `docker-compose.yml` |
| App images | `gateway-service/Dockerfile`, `ledger-service/Dockerfile` |
| In-container config | `application-docker.yml` (per service) |
| CI pipeline | `.github/workflows/ci.yml` |
| Local load test | `node scripts/load-test.mjs` |

---

## Step 1 — Prerequisites

1. Install [Docker Desktop](https://www.docker.com/products/docker-desktop/) (includes Docker Compose v2).
2. Install **JDK 21** (only if you run the app outside Docker).
3. Optional: [Node.js](https://nodejs.org/) for load tests.

Verify:

```powershell
docker --version
docker compose version
```

---

## Step 2 — Containerization (what each container does)

```
postgres / redis / kafka
         │
    ┌────┴────┐
    │ ledger  │  :8081  (settlement, balance, history)
    └────┬────┘
         │ HTTP + Kafka
    ┌────▼────┐
    │ gateway │  :8080  (POST /v1/payments)
    └─────────┘
```

- **postgres** — shared DB: `transactions` + `accounts`.
- **redis** — idempotency (24h), balance cache, tx dedup.
- **kafka** — `payment.initiated` → settlement → `payment.completed` / `payment.failed`.
- **ledger** — Service B (`ledger-service`, port **8081**).
- **gateway** — Service A (`gateway-service`, port **8080**); calls ledger at `http://ledger:8081` inside Compose/K8s.

---

## Step 3 — Start the entire ecosystem (one command)

From the project root:

```powershell
cd d:\transaction-gateway-service
docker compose up --build
```

Wait until you see the app healthy (~1–2 minutes first time). Then check:

| URL | Purpose |
|-----|---------|
| http://localhost:8080/health | Gateway health |
| http://localhost:8081/health | Ledger health |
| http://localhost:8080/swagger-ui.html | Gateway API docs |
| http://localhost:8081/swagger-ui.html | Ledger API docs |
| http://localhost:8080/v1/payments | POST payment (gateway) |
| http://localhost:8081/v1/accounts/1001/balance?currency=INR | Balance (ledger) |
| http://localhost:8081/v1/history/1001?limit=10 | History (ledger) |

### Balance via HTTP (Service A → Service B)

Before each payment, **Service A** calls **Service B**:

`GET /v1/accounts/{userId}/balance?currency=INR`

Then seeds Redis `balance:{userId}` for the fast validation path.

| Config | Purpose |
|--------|---------|
| `app.gateway.ledger-balance.source=http` | Default — use HTTP reader |
| `app.gateway.ledger-balance.source=jpa` | Monolith fallback (direct DB; not for split deploy) |
| `app.ledger.http.base-url` | Ledger URL (local IDE: `http://localhost:8081`) |

When you split into two containers:

```yaml
gateway:
  environment:
    APP_LEDGER_HTTP_BASE_URL: http://ledger:8081
```

Stop everything:

```powershell
docker compose down
```

Remove DB volume (fresh data):

```powershell
docker compose down -v
```

---

## Step 4 — Two ways to develop

### A) Full stack in Docker (recommended for “production-like”)

```powershell
docker compose up --build
```

App uses `application-docker.yml`: hosts `postgres`, `redis`, `kafka:29092`.

### B) Infra in Docker, app in IDE (fast debugging)

```powershell
docker compose up postgres redis kafka
```

Run `LedgerApplication` (8081) then `GatewayApplication` (8080) with **no** `docker` profile (`application.yml` → `localhost` for infra; gateway `app.ledger.http.base-url: http://localhost:8081`).

---

## Step 5 — Balance remaining (accounts in orchestrated Postgres)

Seed data (`data.sql`) gives user **1001** only **10,000 INR**. Load tests exhaust that quickly.

**Check balances:**

```powershell
docker compose exec postgres psql -U postgres -d swiftpay -c "SELECT user_id, balance, currency FROM accounts;"
```

**Top up sender for load / demo:**

```powershell
docker compose exec postgres psql -U postgres -d swiftpay -c "UPDATE accounts SET balance = 100000000 WHERE user_id = 1001;"
```

**Refresh Redis** after DB change (either restart app or POST a payment so `BalanceCacheRefresher` runs):

```powershell
docker compose restart app
```

---

## Step 6 — CI/CD (GitHub Actions)

Pipeline file: `.github/workflows/ci.yml`

On every push/PR to `main`, `master`, or `develop`:

| Job | What it does |
|-----|----------------|
| **compile-and-unit-test** | `./mvnw package` + unit tests (no Docker) |
| **integration-test** | `docker compose up` postgres/redis/kafka → `PaymentApiIntegrationTest` |
| **docker-build** | `docker compose up --build` + health checks on gateway and ledger |

**Enable on GitHub:**

1. Push this repo to GitHub.
2. Open **Actions** — workflow **CI** should run automatically.
3. Fix any failures from the run log (usually Kafka slow start → increase wait loop).

**Optional:** push image to GHCR — add a login + `docker push` step when you have a registry.

---

## Step 7 — Build and test locally (same as CI)

**Unit tests only:**

```powershell
.\mvnw.cmd test -Dtest="!PaymentApiIntegrationTest"
```

**Integration test (start infra first):**

```powershell
docker compose up -d postgres redis kafka
.\mvnw.cmd test -Dtest=PaymentApiIntegrationTest
```

**Build JAR:**

```powershell
.\mvnw.cmd package -DskipTests
```

**Build image only:**

```powershell
docker build -f gateway-service/Dockerfile -t swiftpay-gateway:local .
docker build -f ledger-service/Dockerfile -t swiftpay-ledger:local .
```

---

## Step 8 — Load test in orchestrated environment

```powershell
docker compose up -d --build
# Top up balance (Step 5), then:
node scripts/load-test.mjs --tps 50 --total 10000
```

Start with lower TPS until DB/Kafka pools are tuned; raise sender balance before aiming for 250 TPS × 1M.

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| App won’t start, Kafka errors | `docker compose logs kafka` — wait for health; retry `docker compose up` |
| `Connection refused` to Kafka from IDE | Use `localhost:9092` on host; in Compose use `kafka:29092` (see `application-docker.yml`) |
| 422 insufficient funds | Top up `accounts` (Step 5) |
| Port 8080/8081 in use | Stop local Java or change compose `ports` mapping |
| Gateway cannot reach ledger | Set `APP_LEDGER_HTTP_BASE_URL=http://ledger:8081` (Compose/K8s) |
| CI integration job fails | Ensure `PaymentApiIntegrationTest` runs after compose health checks pass |

---

## Quick reference

```powershell
# Full ecosystem
docker compose up --build

# Infra only
docker compose up postgres redis kafka

# Logs
docker compose logs -f gateway ledger

# DB shell
docker compose exec postgres psql -U postgres -d swiftpay
```
