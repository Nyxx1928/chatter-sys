# Fix Database Permissions Script
# This script grants the necessary permissions to chatuser

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Fixing Database Permissions" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "This script will:" -ForegroundColor Yellow
Write-Host "  1. Grant schema permissions to chatuser" -ForegroundColor Yellow
Write-Host "  2. Set default privileges for future tables" -ForegroundColor Yellow
Write-Host "  3. Verify the permissions" -ForegroundColor Yellow
Write-Host ""

Write-Host "You will be prompted for the postgres password." -ForegroundColor Yellow
Write-Host ""

# Grant schema permissions
Write-Host "Step 1: Granting schema permissions..." -ForegroundColor Cyan
psql -U postgres -h localhost -d chatdb -c "GRANT ALL ON SCHEMA public TO chatuser;"

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to grant schema permissions!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Schema permissions granted" -ForegroundColor Green
Write-Host ""

# Set default privileges
Write-Host "Step 2: Setting default privileges..." -ForegroundColor Cyan
psql -U postgres -h localhost -d chatdb -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO chatuser;"

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to set default privileges!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Default privileges set" -ForegroundColor Green
Write-Host ""

# Grant privileges on existing tables (if any)
Write-Host "Step 3: Granting privileges on existing tables..." -ForegroundColor Cyan
psql -U postgres -h localhost -d chatdb -c "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO chatuser;"

if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Warning: Could not grant privileges on existing tables (this is OK if no tables exist yet)" -ForegroundColor Yellow
} else {
    Write-Host "✅ Privileges granted on existing tables" -ForegroundColor Green
}

Write-Host ""

# Grant sequence privileges
Write-Host "Step 4: Granting sequence privileges..." -ForegroundColor Cyan
psql -U postgres -h localhost -d chatdb -c "GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO chatuser;"

if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Warning: Could not grant sequence privileges (this is OK if no sequences exist yet)" -ForegroundColor Yellow
} else {
    Write-Host "✅ Sequence privileges granted" -ForegroundColor Green
}

Write-Host ""

# Verify permissions
Write-Host "Step 5: Verifying permissions..." -ForegroundColor Cyan
$result = psql -U postgres -h localhost -d chatdb -c "\dn+ public" 2>&1

if ($result -match "chatuser") {
    Write-Host "✅ Permissions verified!" -ForegroundColor Green
} else {
    Write-Host "⚠️  Could not verify permissions, but they should be set" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✅ Database permissions fixed!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor White
Write-Host "  1. Restart your backend server (Ctrl+C and run again)" -ForegroundColor White
Write-Host "  2. The tables will be created automatically" -ForegroundColor White
Write-Host "  3. Run the test script again: powershell -ExecutionPolicy Bypass -File test-backend.ps1" -ForegroundColor White
