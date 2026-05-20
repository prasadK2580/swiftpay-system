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

**Git:** `*-summary.json` and `*-pcap-manifest.json` are in git. The `.pcapng` (~1 GB) is stored with **Git LFS** (`git lfs pull` after clone). Progress `.log` files stay local only.

If LFS bandwidth/storage is unavailable, keep the PCAP locally at `evidence/load-test-YYYYMMDD-HHmmss.pcapng` or attach it to a [GitHub Release](https://github.com/prasadK2580/swiftpay-system/releases).
