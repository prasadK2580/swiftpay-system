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

- **postgres** — shared DB: `transactions` + `accounts`. On **first** empty volume, runs `ledger-service/src/main/resources/schema.sql` and `data.sql` from `/docker-entrypoint-initdb.d/` (compose volume mounts). Apps use `prod` profile → they do **not** run SQL init themselves.
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

Wait until you see the app healthy (~1–2 minutes first time). Postgres init scripts run only when the `swiftpay_pg_data` volume is new; use `docker compose down -v` before `up` if you need a clean schema + seed.

Then check:

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

Remove DB volume (fresh schema + seed on next `up`):

```powershell
docker compose down -v
```

**Manual seed** (infra-only compose — same as CI integration job):

```powershell
docker compose up -d postgres redis kafka
Get-Content ledger-service\src\main\resources\schema.sql | docker compose exec -T postgres psql -U postgres -d swiftpay -v ON_ERROR_STOP=1
Get-Content ledger-service\src\main\resources\data.sql | docker compose exec -T postgres psql -U postgres -d swiftpay -v ON_ERROR_STOP=1
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

**Refresh Redis** after DB change (restart apps or POST a payment so balance cache refreshes):

```powershell
docker compose restart gateway ledger
```

---

## Step 6 — CI/CD (GitHub Actions)

Pipeline file: `.github/workflows/ci.yml`

On every push/PR to `main`, `master`, or `develop`:

| Job | What it does |
|-----|----------------|
| **compile-and-unit-test** | `./mvnw clean install -DskipTests`; `./mvnw test -pl gateway-service -Dtest=PaymentRequestValidatorTest` (parent Surefire excludes `*IntegrationTest`; ledger has no unit tests) |
| **integration-test** | Start postgres/redis/kafka → wait → `psql` apply `schema.sql` + `data.sql` → `./mvnw test -pl ledger-service,gateway-service -Dtest='*IntegrationTest' -Dspring.profiles.active=integration-test` → `docker compose down -v` |
| **docker-build** | `docker compose up -d --build` → poll gateway and ledger `/health` → `docker compose down -v` |

**Enable on GitHub:**

1. Push this repo to GitHub.
2. Open **Actions** — workflow **CI** should run automatically.
3. Fix any failures from the run log (usually Kafka slow start, or ledger exit if Postgres was not seeded).

**Optional:** push image to GHCR — add a login + `docker push` step when you have a registry.

---

## Step 7 — Build and test locally (same as CI)

**Unit tests only:**

```powershell
.\mvnw.cmd -B clean install -DskipTests
.\mvnw.cmd -B test -pl gateway-service -Dtest=PaymentRequestValidatorTest
```

**Integration tests (start infra + seed DB first):**

```powershell
docker compose up -d postgres redis kafka
Get-Content ledger-service\src\main\resources\schema.sql | docker compose exec -T postgres psql -U postgres -d swiftpay -v ON_ERROR_STOP=1
Get-Content ledger-service\src\main\resources\data.sql | docker compose exec -T postgres psql -U postgres -d swiftpay -v ON_ERROR_STOP=1
.\mvnw.cmd -B clean install -DskipTests
.\mvnw.cmd -B test -pl ledger-service,gateway-service -Dtest="*IntegrationTest" -Dspring.profiles.active=integration-test
```

Gateway E2E tests use `IntegrationTestBase` (in-process ledger + unique Kafka groups). No Testcontainers.

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
| **Ledger container exits (1)** on `docker compose up` | Empty Postgres + `prod` profile → `docker compose down -v` then `up` again so init scripts apply; verify compose postgres volume mounts |
| App won’t start, Kafka errors | `docker compose logs kafka` — wait for health; retry `docker compose up` |
| `Connection refused` to Kafka from IDE | Use `localhost:9092` on host; in Compose use `kafka:29092` (see `application-docker.yml`) |
| 422 insufficient funds | Top up `accounts` (Step 5) |
| Port 8080/8081 in use | Stop local Java or change compose `ports` mapping |
| Gateway cannot reach ledger | Set `APP_LEDGER_HTTP_BASE_URL=http://ledger:8081` (Compose/K8s) |
| CI integration job fails | Ensure infra healthy and `schema.sql` / `data.sql` seeded before Maven |
| CI docker-build fails | Check `docker compose logs ledger` — usually missing DB schema on first init |

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
