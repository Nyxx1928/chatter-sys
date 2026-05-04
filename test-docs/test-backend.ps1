# Backend Test Script for Windows PowerShell
# Tests if the Spring Boot backend is running and accessible

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Backend Health Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080"

# Test 1: Check if server is responding
Write-Host "Test 1: Checking if server is running..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri $baseUrl -Method GET -ErrorAction Stop
    Write-Host "✅ Server is running!" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode -eq 403) {
        Write-Host "✅ Server is running! (403 Forbidden is expected for root endpoint)" -ForegroundColor Green
    } elseif ($_.Exception.Message -like "*Unable to connect*") {
        Write-Host "❌ Server is NOT running!" -ForegroundColor Red
        Write-Host "Please start the backend with: mvn spring-boot:run" -ForegroundColor Yellow
        exit 1
    } else {
        Write-Host "⚠️  Unexpected response: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

Write-Host ""

# Test 2: Register a new user
Write-Host "Test 2: Registering a test user..." -ForegroundColor Yellow
$registerBody = @{
    username = "testuser_$(Get-Random -Maximum 10000)"
    email = "test_$(Get-Random -Maximum 10000)@example.com"
    password = "password123"
    displayName = "Test User"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" `
        -Method Post `
        -ContentType "application/json" `
        -Body $registerBody `
        -ErrorAction Stop
    
    Write-Host "✅ User registered successfully!" -ForegroundColor Green
    Write-Host "   Username: $($registerResponse.username)" -ForegroundColor Gray
    Write-Host "   Display Name: $($registerResponse.displayName)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Registration failed!" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.ErrorDetails.Message) {
        Write-Host "   Details: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "Common issues:" -ForegroundColor Yellow
    Write-Host "  1. Database not running or not accessible" -ForegroundColor Yellow
    Write-Host "  2. Database credentials incorrect" -ForegroundColor Yellow
    Write-Host "  3. Check logs/chat-application.log for details" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Test 3: Login with the user
Write-Host "Test 3: Logging in..." -ForegroundColor Yellow
$loginBody = @{
    username = $registerResponse.username
    password = "password123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $loginBody `
        -ErrorAction Stop
    
    Write-Host "✅ Login successful!" -ForegroundColor Green
    Write-Host "   Token: $($loginResponse.token.Substring(0, 50))..." -ForegroundColor Gray
    
    $token = $loginResponse.token
} catch {
    Write-Host "❌ Login failed!" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Test 4: Get current user profile
Write-Host "Test 4: Getting user profile..." -ForegroundColor Yellow
$headers = @{
    "Authorization" = "Bearer $token"
}

try {
    $profileResponse = Invoke-RestMethod -Uri "$baseUrl/api/users/me" `
        -Method Get `
        -Headers $headers `
        -ErrorAction Stop
    
    Write-Host "✅ Profile retrieved successfully!" -ForegroundColor Green
    Write-Host "   Username: $($profileResponse.username)" -ForegroundColor Gray
    Write-Host "   Email: $($profileResponse.email)" -ForegroundColor Gray
    Write-Host "   Online: $($profileResponse.online)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Profile retrieval failed!" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Test 5: Create a chat room
Write-Host "Test 5: Creating a chat room..." -ForegroundColor Yellow
$roomBody = @{
    name = "Test Room $(Get-Random -Maximum 10000)"
    description = "A test chat room"
} | ConvertTo-Json

try {
    $roomResponse = Invoke-RestMethod -Uri "$baseUrl/api/rooms" `
        -Method Post `
        -ContentType "application/json" `
        -Headers $headers `
        -Body $roomBody `
        -ErrorAction Stop
    
    Write-Host "✅ Room created successfully!" -ForegroundColor Green
    Write-Host "   Room ID: $($roomResponse.id)" -ForegroundColor Gray
    Write-Host "   Room Name: $($roomResponse.name)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Room creation failed!" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✅ All tests passed!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Your backend is working correctly!" -ForegroundColor Green
Write-Host "You can now:" -ForegroundColor White
Write-Host "  1. Test the API using the commands in API_TESTING.md" -ForegroundColor White
Write-Host "  2. Start the frontend (cd frontend && npm run dev)" -ForegroundColor White
Write-Host "  3. Access the application at http://localhost:3000" -ForegroundColor White
