# Run Service A (gateway) from repo root. Requires: postgres, redis, kafka, ledger on :8081
Set-Location $PSScriptRoot\..

Write-Host "Building swiftpay-shared + gateway-service..."
& .\mvnw.cmd -pl gateway-service -am install "-Dmaven.test.skip=true" -q
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Starting gateway on http://localhost:8080 (ledger must be at http://localhost:8081)"
& .\mvnw.cmd -pl gateway-service spring-boot:run "-Dmaven.test.skip=true"
