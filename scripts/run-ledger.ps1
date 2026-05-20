# Run Service B (ledger) from repo root. Requires: postgres, redis, kafka
Set-Location $PSScriptRoot\..

Write-Host "Building swiftpay-shared + ledger-service..."
& .\mvnw.cmd -pl ledger-service -am install "-Dmaven.test.skip=true" -q
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Starting ledger on http://localhost:8081"
& .\mvnw.cmd -pl ledger-service spring-boot:run "-Dmaven.test.skip=true"
