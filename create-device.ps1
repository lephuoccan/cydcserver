# Script to create account, device and get token
# Usage: .\create-device.ps1

$baseUrl = "http://localhost:8081/api"

Write-Host "=== CYDConnect Device Setup ===" -ForegroundColor Cyan
Write-Host ""

# Step 1: Register user
Write-Host "[1/3] Registering user..." -ForegroundColor Yellow
$userId = "esp32@cydc.local"
$email = "esp32@cydc.local"
$password = "esp32pass123"

$registerBody = @{
    userId = $userId
    email = $email
    pass = $password
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$baseUrl/register" -Method Post -Body $registerBody -ContentType "application/json"
    Write-Host "User registered: $userId" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "User already exists: $userId" -ForegroundColor Green
    } else {
        Write-Host "Registration failed: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""

# Step 2: Create dashboard
Write-Host "[2/3] Creating dashboard..." -ForegroundColor Yellow
$dashId = 0

$dashboardBody = @{
    name = "ESP32 Dashboard"
    theme = "default"
} | ConvertTo-Json

try {
    $dashResponse = Invoke-RestMethod -Uri "$baseUrl/dashboard/$userId" -Method Post -Body $dashboardBody -ContentType "application/json"
    Write-Host "Dashboard created: $($dashResponse.dashboard.name)" -ForegroundColor Green
    $dashId = $dashResponse.dashboard.dashId
} catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "Dashboard already exists" -ForegroundColor Green
    } else {
        Write-Host "Dashboard creation failed: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""

# Step 3: Create device and get token
Write-Host "[3/3] Creating device..." -ForegroundColor Yellow

$deviceBody = @{
    name = "ESP32 Device"
    boardType = "ESP32"
} | ConvertTo-Json

try {
    $deviceResponse = Invoke-RestMethod -Uri "$baseUrl/device/$userId/$dashId" -Method Post -Body $deviceBody -ContentType "application/json"
    $token = $deviceResponse.device.token
    $devId = $deviceResponse.device.devId
    
    Write-Host "Device created successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "=== Device Information ===" -ForegroundColor Cyan
    Write-Host "User ID  : $userId" -ForegroundColor White
    Write-Host "Dashboard: $dashId" -ForegroundColor White
    Write-Host "Device ID: $devId" -ForegroundColor White
    Write-Host "Token    : $token" -ForegroundColor Green
    Write-Host ""
    Write-Host "=== ESP32 Arduino Code ===" -ForegroundColor Cyan
    $codeSnippet = @"
char auth[] = "$token";
char server[] = "192.168.1.100";
int port = 8442;

void setup() {
    Serial.begin(115200);
    Blynk.begin(auth, ssid, pass, server, port);
}
"@
    Write-Host $codeSnippet -ForegroundColor Yellow
    
    Write-Host ""
    Write-Host "Token copied to clipboard!" -ForegroundColor Green
    Set-Clipboard -Value $token
    
} catch {
    Write-Host "Device creation failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host "Response: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "=== Test Commands ===" -ForegroundColor Cyan
Write-Host "# Read pin V1:" -ForegroundColor Gray
Write-Host "curl http://localhost:8081/api/pin/$devId/1" -ForegroundColor White
Write-Host ""
Write-Host "# Write pin V1:" -ForegroundColor Gray
Write-Host "curl -X POST http://localhost:8081/api/pin/$devId/1 -H 'Content-Type: application/json' -d '{`"value`":`"123`"}'" -ForegroundColor White
Write-Host ""
