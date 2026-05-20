#Requires -RunAsAdministrator
<#
.SYNOPSIS
  Load test: 250 TPS x 1,000,000 payments + PCAP evidence (PktMon on Windows).

.USAGE
  Open PowerShell as Administrator, then:
    cd d:\transaction-gateway-service
    .\scripts\run-load-test-1m-with-pcap.ps1

  Optional env:
    $env:SKIP_PCAP = "1"     # run load test only (no packet capture)
    $env:TPS = "250"
    $env:TOTAL = "1000000"
#>
$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

$Evidence = Join-Path $Root "evidence"
New-Item -ItemType Directory -Force -Path $Evidence | Out-Null

$Tps = if ($env:TPS) { $env:TPS } else { "250" }
$Total = if ($env:TOTAL) { $env:TOTAL } else { "1000000" }
$RunId = Get-Date -Format "yyyyMMdd-HHmmss"
$LogFile = Join-Path $Evidence "load-test-$RunId.log"
$JsonFile = Join-Path $Evidence "load-test-$RunId-summary.json"
$PcapFile = Join-Path $Evidence "load-test-$RunId.pcapng"
$EtlFile = Join-Path $Evidence "load-test-$RunId.etl"

Write-Host "=== SwiftPay 1M load test ===" -ForegroundColor Cyan
Write-Host "TPS=$Tps TOTAL=$Total"
Write-Host "Log: $LogFile"
Write-Host "Summary JSON: $JsonFile"
Write-Host "PCAP: $PcapFile"
Write-Host ""

# Health check
try {
    $h = Invoke-RestMethod -Uri "http://localhost:8080/health" -TimeoutSec 5
    if ($h.status -ne "UP") { throw "Health not UP" }
    Write-Host "App health: UP" -ForegroundColor Green
} catch {
    Write-Error "Start the app first (docker compose up or IDE). $_"
}

$PktmonStarted = $false
if ($env:SKIP_PCAP -ne "1") {
    Write-Host "Starting PktMon capture (TCP port 8080)..." -ForegroundColor Yellow
    pktmon filter remove 2>$null
    pktmon filter add -p 8080 -t TCP 2>&1 | Out-Null
    pktmon start --etw -m circular -f $EtlFile -c 512 2>&1 | Out-Null
    $PktmonStarted = $true
    Write-Host "PktMon recording to $EtlFile"
} else {
    Write-Host "SKIP_PCAP=1 — no packet capture" -ForegroundColor Yellow
}

$LoadStart = Get-Date
Write-Host "Load test started at $LoadStart (~$([math]::Round($Total / $Tps / 60, 1)) minutes expected)"

try {
    Push-Location $Root
    node scripts/load-test.mjs --tps $Tps --total $Total --summary $JsonFile 2>&1 | Tee-Object -FilePath $LogFile
} finally {
    Pop-Location
    if ($PktmonStarted) {
        Write-Host "Stopping PktMon and converting to PCAP..." -ForegroundColor Yellow
        pktmon stop 2>&1 | Out-Null
        if (Test-Path $EtlFile) {
            pktmon etl2pcap $EtlFile -o $PcapFile 2>&1 | Out-Null
            Write-Host "PCAP written: $PcapFile" -ForegroundColor Green
        }
    }
}

Write-Host ""
Write-Host "Done. Submit for evidence:" -ForegroundColor Cyan
Write-Host "  - $PcapFile (or $EtlFile if conversion failed)"
Write-Host "  - $LogFile"
Write-Host "  - $JsonFile"
Write-Host "  - docs/LOAD-TEST-EVIDENCE.md (bottleneck notes)"
