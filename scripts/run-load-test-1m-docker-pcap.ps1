# Requires: Docker Desktop, Node.js, swiftpay-gateway container running (see docker-compose).
# Records HTTP to gateway :8080 into evidence/*.pcapng via a container sharing the gateway network namespace.
# Usage:
#   cd d:\transaction-gateway-service
#   docker compose up -d --build
#   .\scripts\run-load-test-1m-docker-pcap.ps1
# Optional env: $env:TPS="250" $env:TOTAL="1000000" $env:PCAP_SNAPLEN="0" (0 = full packets)
$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Evidence = Join-Path $Root "evidence"
New-Item -ItemType Directory -Force -Path $Evidence | Out-Null

$Tps = if ($env:TPS) { $env:TPS } else { "250" }
$Total = if ($env:TOTAL) { $env:TOTAL } else { "1000000" }
$Snap = if ($env:PCAP_SNAPLEN) { $env:PCAP_SNAPLEN } else { "0" }

$RunStamp = Get-Date -Format "yyyyMMdd-HHmmss"
$FileName = "load-test-$RunStamp.pcapng"
$PcapAbs = Join-Path $Evidence $FileName
$LogFile = Join-Path $Evidence "load-test-$RunStamp.log"
$JsonFile = Join-Path $Evidence "load-test-$RunStamp-summary.json"
$DumpName = "swiftpay-tcpdump-$RunStamp"

Write-Host "=== SwiftPay load test + Docker tcpdump (.pcapng) ===" -ForegroundColor Cyan
Write-Host "TPS=$Tps TOTAL=$Total PCAP_SNAPLEN=$Snap"
Write-Host "Log: $LogFile"
Write-Host "Summary JSON: $JsonFile"
Write-Host "PCAP: $PcapAbs"
Write-Host ""

$gwLine = docker ps --filter "name=swiftpay-gateway" --format "{{.Names}}" 2>$null
if (-not $gwLine) {
    Write-Error "Container swiftpay-gateway is not running. Run: docker compose up -d --build"
}

$healthTry = 0
while ($healthTry -lt 60) {
    try {
        # 127.0.0.1 avoids slow/failed ::1 (localhost) connects on some Windows stacks
        $h = Invoke-RestMethod -Uri "http://127.0.0.1:8080/health" -TimeoutSec 20
        if ($h.status -eq "UP") { break }
    } catch {}
    Start-Sleep -Seconds 5
    $healthTry++
}
if ($healthTry -ge 60) { Write-Error "Gateway http://127.0.0.1:8080/health did not become UP in time." }
Write-Host "App health: UP" -ForegroundColor Green

Write-Host "Starting tcpdump sidecar (network=container:swiftpay-gateway, tcp port 8080)..."
$PrevEap = $ErrorActionPreference
$ErrorActionPreference = "SilentlyContinue"
docker rm -f $DumpName 2>$null | Out-Null
$ErrorActionPreference = $PrevEap

# Netshoot image: tcpdump on shared netns with gateway; -U packet-buffered flush for timely writes
$dId = docker run -d `
    --name $DumpName `
    --network "container:swiftpay-gateway" `
    -v "${Evidence}:/capture" `
    nicolaka/netshoot `
    tcpdump -i any -U -nn -s $Snap -w "/capture/$FileName" tcp port 8080

Write-Host "tcpdump container: $DumpName ($dId)"
Write-Host $PcapAbs
Start-Sleep -Seconds 2

$LoadStart = Get-Date
$expectMin = [math]::Round([double]$Total / [double]$Tps / 60, 1)
Write-Host "Load test started at $LoadStart (~$expectMin minutes expected)"

try {
    Push-Location $Root
    $env:SUMMARY_PATH = $JsonFile
    node scripts/load-test.mjs --tps $Tps --total $Total --summary $JsonFile 2>&1 | Tee-Object -FilePath $LogFile
}
finally {
    Pop-Location
    Write-Host "Stopping tcpdump container (flushes pcapng)..." -ForegroundColor Yellow
    $PrevEap = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    docker stop $DumpName 2>$null | Out-Null
    docker rm $DumpName 2>$null | Out-Null
    $ErrorActionPreference = $PrevEap
}

if (Test-Path $PcapAbs) {
    Write-Host "PCAP written: $PcapAbs" -ForegroundColor Green
} else {
    Write-Warning "PCAP missing at $PcapAbs - check docker volume mount and tcpdump logs."
}

Write-Host ""
Write-Host "Done. Evidence:" -ForegroundColor Cyan
Write-Host ('  ' + $PcapAbs)
Write-Host ('  ' + $LogFile)
Write-Host ('  ' + $JsonFile)
