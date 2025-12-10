# Test script for CYDC Server API
$baseUrl = "http://localhost:8081/api"

Write-Host "=== Testing CYDC Server API ===" -ForegroundColor Cyan

# Test 1: Health Check
Write-Host "`n1. Testing Health Endpoint..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/health" -Method GET -UseBasicParsing
    Write-Host "Success: $($response.StatusCode) - $($response.Content)" -ForegroundColor Green
} catch {
    Write-Host "Failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: Register User
Write-Host "`n2. Testing User Registration..." -ForegroundColor Yellow
$registerBody = @{
    email = "testuser@example.com"
    appName = "TestApp"
    password = "password123"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/users/register" -Method POST -ContentType "application/json" -Body $registerBody -UseBasicParsing
    Write-Host "Success: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "Response: $($response.Content)" -ForegroundColor Gray
} catch {
    Write-Host "Failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Login
Write-Host "`n3. Testing User Login..." -ForegroundColor Yellow
$loginBody = @{
    email = "testuser@example.com"
    appName = "TestApp"
    password = "password123"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/users/login" -Method POST -ContentType "application/json" -Body $loginBody -UseBasicParsing
    Write-Host "✓ User Login: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "  Response: $($response.Content)" -ForegroundColor Gray
    $userId = ($response.Content | ConvertFrom-Json).userId
} catch {
    Write-Host "✗ Login Failed: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

# Test 4: Create Dashboard
Write-Host "`n4. Testing Dashboard Creation..." -ForegroundColor Yellow
$dashboardBody = @{
    userId = $userId
    name = "Home Dashboard"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/dashboards" -Method POST -ContentType "application/json" -Body $dashboardBody -UseBasicParsing
    Write-Host "✓ Dashboard Created: $($response.StatusCode)" -ForegroundColor Green
    $dashboardData = $response.Content | ConvertFrom-Json
    $dashboardId = $dashboardData.id
    Write-Host "  Dashboard ID: $dashboardId" -ForegroundColor Gray
} catch {
    Write-Host "✗ Dashboard Creation Failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 5: Create Device
Write-Host "`n5. Testing Device Creation..." -ForegroundColor Yellow
$deviceBody = @{
    dashboardId = $dashboardId
    deviceId = 101
    name = "Arduino Device"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/devices" -Method POST -ContentType "application/json" -Body $deviceBody -UseBasicParsing
    Write-Host "✓ Device Created: $($response.StatusCode)" -ForegroundColor Green
    $deviceData = $response.Content | ConvertFrom-Json
    $token = $deviceData.token
    Write-Host "  Token: $token" -ForegroundColor Gray
} catch {
    Write-Host "✗ Device Creation Failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 6: Write Pin (with token)
Write-Host "`n6. Testing Pin Write (with auth token)..." -ForegroundColor Yellow
$pinBody = @{
    pin = "V1"
    value = "25.5"
} | ConvertTo-Json

try {
    $headers = @{
        "Authorization" = "Bearer $token"
    }
    $response = Invoke-WebRequest -Uri "$baseUrl/pins" -Method PUT -ContentType "application/json" -Body $pinBody -Headers $headers -UseBasicParsing
    Write-Host "✓ Pin Write: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "  Response: $($response.Content)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Pin Write Failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        Write-Host "  Status Code: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
    }
}

# Test 7: Read Pin
Write-Host "`n7. Testing Pin Read..." -ForegroundColor Yellow
try {
    $headers = @{
        "Authorization" = "Bearer $token"
    }
    $response = Invoke-WebRequest -Uri "$baseUrl/pins/V1" -Method GET -Headers $headers -UseBasicParsing
    Write-Host "✓ Pin Read: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "  Value: $($response.Content)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Pin Read Failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== Test Suite Complete ===" -ForegroundColor Cyan
