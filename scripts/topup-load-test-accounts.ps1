# Top up ledger accounts for load tests. Values from scripts/load-test-accounts.config.json
# (override path with -ConfigPath or env SWIFTPAY_LOAD_TEST_ACCOUNTS_CONFIG).
#
# Usage:
#   .\scripts\topup-load-test-accounts.ps1
#   $env:SWIFTPAY_LOAD_TEST_SENDER_ID = "1001"; .\scripts\topup-load-test-accounts.ps1
param(
    [string]$ConfigPath = $(if ($env:SWIFTPAY_LOAD_TEST_ACCOUNTS_CONFIG) { $env:SWIFTPAY_LOAD_TEST_ACCOUNTS_CONFIG } else { "" })
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
if (-not $ConfigPath) {
    $ConfigPath = Join-Path $PSScriptRoot "load-test-accounts.config.json"
}
if (-not (Test-Path $ConfigPath)) {
    Write-Error "Config not found: $ConfigPath"
}

$cfg = Get-Content $ConfigPath -Raw | ConvertFrom-Json

$senderId = if ($env:SWIFTPAY_LOAD_TEST_SENDER_ID) { [long]$env:SWIFTPAY_LOAD_TEST_SENDER_ID } else { [long]$cfg.loadTestSenderId }
$receiverId = if ($env:SWIFTPAY_LOAD_TEST_RECEIVER_ID) { [long]$env:SWIFTPAY_LOAD_TEST_RECEIVER_ID } else { [long]$cfg.loadTestReceiverId }
$currency = if ($env:SWIFTPAY_LOAD_TEST_CURRENCY) { $env:SWIFTPAY_LOAD_TEST_CURRENCY } else { $cfg.currency }
$topupBalance = if ($env:SWIFTPAY_LOAD_TEST_TOPUP_BALANCE) { $env:SWIFTPAY_LOAD_TEST_TOPUP_BALANCE } else { [string]$cfg.topupBalance }
$ledgerBase = if ($env:APP_LEDGER_HTTP_BASE_URL) { $env:APP_LEDGER_HTTP_BASE_URL.TrimEnd('/') } else { $cfg.ledger.baseUrl.TrimEnd('/') }
$pgContainer = if ($env:SWIFTPAY_POSTGRES_CONTAINER) { $env:SWIFTPAY_POSTGRES_CONTAINER } else { $cfg.postgres.container }
$pgDb = if ($env:SWIFTPAY_POSTGRES_DATABASE) { $env:SWIFTPAY_POSTGRES_DATABASE } else { $cfg.postgres.database }
$pgUser = if ($env:SWIFTPAY_POSTGRES_USER) { $env:SWIFTPAY_POSTGRES_USER } else { $cfg.postgres.user }
$redisContainer = if ($env:SWIFTPAY_REDIS_CONTAINER) { $env:SWIFTPAY_REDIS_CONTAINER } else { $cfg.redis.container }

function Invoke-PostgresSql([string]$Sql) {
    $Sql | docker exec -i $pgContainer psql -U $pgUser -d $pgDb -v ON_ERROR_STOP=1
}

function Ensure-ExtraAccounts {
    $startId = if ($env:SWIFTPAY_EXTRA_ACCOUNTS_START_ID) { [long]$env:SWIFTPAY_EXTRA_ACCOUNTS_START_ID } else { [long]$cfg.extraAccounts.startId }
    $count = if ($env:SWIFTPAY_EXTRA_ACCOUNTS_COUNT) { [int]$env:SWIFTPAY_EXTRA_ACCOUNTS_COUNT } else { [int]$cfg.extraAccounts.count }
    $balance = if ($env:SWIFTPAY_EXTRA_ACCOUNTS_BALANCE) { [double]$env:SWIFTPAY_EXTRA_ACCOUNTS_BALANCE } else { [double]$cfg.extraAccounts.initialBalance }
    if ($count -le 0) { return }

    $endId = $startId + $count - 1
    Write-Host "Seeding extra accounts $startId-$endId ($currency, balance=$balance)..." -ForegroundColor Cyan

    $values = @()
    for ($id = $startId; $id -le $endId; $id++) {
        $values += "($id, $balance, '$currency')"
    }
    $insert = @"
INSERT INTO accounts (user_id, balance, currency)
VALUES $($values -join ",`n       ")
ON CONFLICT (user_id) DO NOTHING;

SELECT user_id, balance, currency FROM accounts WHERE user_id BETWEEN $startId AND $endId ORDER BY user_id;
"@
    Invoke-PostgresSql $insert
}

if (-not (docker ps --filter "name=$pgContainer" --format "{{.Names}}" 2>$null)) {
    Write-Error "Postgres container '$pgContainer' is not running. Start stack: docker compose up -d"
}

Ensure-ExtraAccounts

Write-Host "Topping up sender $senderId in Postgres (balance=$topupBalance)..." -ForegroundColor Cyan
$topupSql = Join-Path $Root "scripts\sql\ensure-load-test-balance.sql"
Get-Content $topupSql | docker exec -i $pgContainer psql -U $pgUser -d $pgDb -v ON_ERROR_STOP=1 `
    -v sender_id=$senderId -v receiver_id=$receiverId -v topup_balance=$topupBalance

Write-Host "Clearing stale gateway balance cache in Redis..." -ForegroundColor Cyan
docker exec $redisContainer redis-cli DEL "balance:$senderId" "balance:$receiverId" | Out-Null

$balanceUrl = "$ledgerBase/v1/accounts/$senderId/balance?currency=$currency"
$balance = Invoke-RestMethod -Uri $balanceUrl -TimeoutSec 15
Write-Host "Sender $senderId balance: $($balance.balance) $($balance.currency)" -ForegroundColor Green
