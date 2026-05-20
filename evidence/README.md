# Load test evidence (submission)

Place artifacts here after running the 1M @ 250 TPS test:

| File | Description |
|------|-------------|
| `load-test-YYYYMMDD-HHmmss.pcapng` | PCAP from PktMon (HTTP to `:8080`) |
| `load-test-YYYYMMDD-HHmmss.log` | Console progress log |
| `load-test-YYYYMMDD-HHmmss-summary.json` | Metrics JSON |

Generate with (PowerShell **as Administrator**):

```powershell
cd d:\transaction-gateway-service
.\scripts\run-load-test-1m-with-pcap.ps1
```

See `docs/LOAD-TEST-EVIDENCE.md` for methodology and bottleneck analysis.
