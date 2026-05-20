# Load test evidence — 250 TPS × 1,000,000 transactions

## Requirement

| Item | Target |
|------|--------|
| Throughput | **250 TPS** (sustained arrival rate) |
| Volume | **1,000,000** HTTP `POST /v1/payments` |
| Evidence | **PCAP** of traffic during the run |
| Analysis | **Bottleneck identification** + balance impact |

Expected duration: **1,000,000 ÷ 250 ≈ 4,000 s (~66.7 min)**.

---

## Step 1 — Preconditions

1. **Stack running** — gateway http://localhost:8080/health and ledger http://localhost:8081/health (`docker compose up --build` uses `docker,performance` profiles)  
2. **Sender balance** — user `1001` needs **≥ 1,000,000 INR** (amount=1 per tx) for all `201` responses:

```sql
SELECT user_id, balance, currency FROM accounts WHERE user_id IN (1001, 2002);
UPDATE accounts SET balance = 2000000000 WHERE user_id = 1001;
```

3. **Disk space** — PCAP + logs can be **hundreds of MB to several GB**.

---

## Step 2 — Run test + PCAP (one script)

**PowerShell as Administrator** (required for PktMon on Windows):

```powershell
cd d:\transaction-gateway-service
.\scripts\run-load-test-1m-with-pcap.ps1
```

Outputs under `evidence/`:

- `load-test-<timestamp>.pcapng` — **submit this PCAP**
- `load-test-<timestamp>.log`
- `load-test-<timestamp>-summary.json`

Without admin / skip PCAP:

```powershell
$env:SKIP_PCAP = "1"
node scripts/load-test.mjs --tps 250 --total 1000000 --summary evidence/load-test-summary.json
```

### Alternative PCAP (Wireshark)

1. Install [Wireshark](https://www.wireshark.org/) (includes `tshark`).
2. Capture loopback: `tshark -i 1 -f "tcp port 8080" -w evidence/load-test.pcapng`
3. Run load test in a second terminal.

---

## Step 3 — What we measure (bottlenecks)

| Layer | Symptom in test | Likely bottleneck |
|-------|------------------|-------------------|
| **Client** | `inFlight` pegged at 2000, achieved TPS &lt; 250 | `max-in-flight` / Node concurrency |
| **HTTP / Tomcat** | Rising latency, thread pool saturated | Server worker threads |
| **Service A** | Many `201` then `422` | **Insufficient balance** on `1001` |
| **PostgreSQL** | Slow `201`, DB CPU high | Connection pool, `transactions` insert rate |
| **Redis** | Idempotency / balance cache latency | Single Redis instance, network |
| **Kafka** | `201` OK but settlement lag | Consumer lag, partition count |
| **Service B** | Hot account lock waits | **Ordered row locks** on `accounts` (contention, not deadlock) |

### Deadlock strategy during load

Settlement uses **lock ordering** (`min(userId)` then `max(userId)`) — **deadlock prevention**. Under 250 TPS, expect **queueing** on hot rows (e.g. receiver `2002`), not deadlocks.

### Is the code “suitable” for 250 TPS × 1M?

| Aspect | Status |
|--------|--------|
| **Architecture** | Yes — async Kafka settlement, Redis idempotency, ordered DB locks |
| **Hackathon HTTP target** | Load script hits **gateway :8080** only; each payment also calls **ledger :8081** for balance |
| **Proven at scale** | **10k @ ~200 TPS** with 0 errors (monolith era); **full 1M not completed** in repo evidence yet |
| **Settlement lag** | At 250 TPS, Kafka/ledger may **lag behind** HTTP `201`; status may stay `PENDING` for minutes — that is expected under load |

**Before 1M:** run `scripts/sql/ensure-load-test-balance.sql`, fresh `docker compose`, enough disk for PCAP/logs.

### Prior run (reference)

| Run | Result |
|-----|--------|
| 1M @ 250 TPS (low balance) | Stalled ~3k `201`, then errors (overload + balance) |
| 10k @ 250 TPS (balance topped up) | **10,000 × 201**, ~200 achieved TPS, 0 errors |

---

## Step 4 — Fill in results (after your 1M run)

From `evidence/load-test-*-summary.json`:

| Metric | Your value |
|--------|------------|
| Target TPS | 250 |
| Achieved TPS | |
| Total completed | |
| HTTP 201 | |
| HTTP 422 | |
| Errors | |
| p50 / p95 / p99 latency (ms) | |
| Duration (min) | |

**Bottleneck conclusion (template):**

> Primary bottleneck observed: _____________  
> Evidence: _____________ (e.g. summary.json line, PCAP filter `http.response.code`, DB metrics)  
> Mitigation considered: _____________  

---

## Step 5 — Submission checklist

- [ ] `evidence/load-test-*.pcapng` (PCAP during load)
- [ ] `evidence/load-test-*.log`
- [ ] `evidence/load-test-*-summary.json`
- [ ] This doc completed with metrics + bottleneck paragraph
- [ ] Screenshot or SQL showing **balance before/after** on `1001` / `2002`

---

## PCAP analysis hints (Wireshark)

Filter examples:

```
tcp.port == 8080
http.response.code
http.request.method == "POST"
```

Verify: ~250 new POSTs/sec average over the capture window (allow burst + client in-flight).
