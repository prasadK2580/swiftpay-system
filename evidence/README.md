# Load test evidence (submission)

Place artifacts here after running the 1M @ 250 TPS test:

| File | Description |
|------|-------------|
| `load-test-YYYYMMDD-HHmmss.pcapng` | PCAP (HTTP / TCP to gateway `:8080`) |
| `load-test-YYYYMMDD-HHmmss.log` | Console progress log |
| `load-test-YYYYMMDD-HHmmss-summary.json` | Metrics JSON |

**Option A — Docker tcpdump (no Administrator):** records traffic on the gateway container’s network stack (load generator on host → published `:8080`).

```powershell
cd d:\transaction-gateway-service
docker compose up -d --build
.\scripts\run-load-test-1m-docker-pcap.ps1
```

**Option B — PktMon (Windows, PowerShell as Administrator):**

```powershell
cd d:\transaction-gateway-service
.\scripts\run-load-test-1m-with-pcap.ps1
```

See `docs/LOAD-TEST-EVIDENCE.md` for methodology and bottleneck analysis.

**Git:** PCAP and `.log` files are not committed (size limits). Commit `*-summary.json` and submit PCAP separately (release asset, drive, etc.).
