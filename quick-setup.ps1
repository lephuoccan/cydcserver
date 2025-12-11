# Quick device creation script
# Creates device directly and gets token

$baseUrl = "http://localhost:8081/api"

Write-Host "=== Quick Device Setup ===" -ForegroundColor Cyan

# Register user (if not exists)
$userId = "esp32"
$email = "esp32@cydc.local"
$pass = "esp32pass123"

Write-Host "Registering user..." -ForegroundColor Yellow
$registerBody = @{
    userId = $userId
    email = $email
    pass = $pass
} | ConvertTo-Json

$actualUserId = $null
try {
    $regResponse = Invoke-RestMethod -Uri "$baseUrl/register" -Method Post -Body $registerBody -ContentType "application/json"
    $actualUserId = $regResponse.userId
    Write-Host "User created: $actualUserId" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        # User exists, construct the actual userId
        $actualUserId = "$email-Blynk"
        Write-Host "User exists: $actualUserId" -ForegroundColor Green
    } else {
        Write-Host "Registration failed: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

if (-not $actualUserId) {
    Write-Host "Failed to get user ID" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Create device
Write-Host "Creating device..." -ForegroundColor Yellow
$dashId = 0
$deviceBody = @{
    name = "ESP32 Device"
    boardType = "ESP32"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/device/$actualUserId/$dashId" -Method Post -Body $deviceBody -ContentType "application/json"
    
    if ($response.token) {
        $token = $response.token
        $devId = $response.id
        
        Write-Host ""
        Write-Host "=== SUCCESS ===" -ForegroundColor Green
        Write-Host "Token: $token" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "=== Arduino Code for ESP32 ===" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "char auth[] = `"$token`";" -ForegroundColor White
        Write-Host "char server[] = `"192.168.1.100`";  // Your server IP" -ForegroundColor White
        Write-Host "int port = 8442;" -ForegroundColor White
        Write-Host ""
        Write-Host "void setup() {" -ForegroundColor White
        Write-Host "    Serial.begin(115200);" -ForegroundColor White
        Write-Host "    Blynk.begin(auth, ssid, pass, server, port);" -ForegroundColor White
        Write-Host "}" -ForegroundColor White
        Write-Host ""
        
        # Copy token to clipboard
        Set-Clipboard -Value $token
        Write-Host "Token copied to clipboard!" -ForegroundColor Green
        Write-Host ""
        
        Write-Host "=== Test Commands ===" -ForegroundColor Cyan
        Write-Host "# Write to virtual pin V1:" -ForegroundColor Gray
        Write-Host "curl -X PUT http://localhost:8081/api/pin/$devId/V1 -H 'Content-Type: application/json' -H 'Authorization: Bearer $token' -d '{`"value`":`"123`"}'" -ForegroundColor White
        Write-Host ""
        Write-Host "# Read from virtual pin V1:" -ForegroundColor Gray
        Write-Host "curl http://localhost:8081/api/pin/$devId/V1?token=$token" -ForegroundColor White
        Write-Host ""
    } else {
        Write-Host "Device creation failed: $($response.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host "Details: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
}
