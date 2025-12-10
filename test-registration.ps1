$BASE_URL = "http://localhost:8081"

Write-Host "=== Test 1: Register user with email and password only ===" -ForegroundColor Green
Invoke-WebRequest -Uri "$BASE_URL/api/register" -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body '{"email":"john@example.com","pass":"mypassword123"}' | Select-Object -ExpandProperty Content
Write-Host "`n"

Write-Host "=== Test 2: Register user with custom appName ===" -ForegroundColor Green
Invoke-WebRequest -Uri "$BASE_URL/api/register" -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body '{"email":"alice@example.com","pass":"pass456","appName":"MyApp"}' | Select-Object -ExpandProperty Content
Write-Host "`n"

Write-Host "=== Test 3: Try to register duplicate (should fail) ===" -ForegroundColor Yellow
try {
  Invoke-WebRequest -Uri "$BASE_URL/api/register" -Method POST `
    -Headers @{"Content-Type" = "application/json"} `
    -Body '{"email":"john@example.com","pass":"different123"}' | Select-Object -ExpandProperty Content
} catch {
  Write-Host $_.Exception.Response.StatusCode $_.Exception.Message
  Write-Host ([System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream()).ReadToEnd())
}
Write-Host "`n"

Write-Host "=== Test 4: Try to register without email (should fail) ===" -ForegroundColor Yellow
try {
  Invoke-WebRequest -Uri "$BASE_URL/api/register" -Method POST `
    -Headers @{"Content-Type" = "application/json"} `
    -Body '{"pass":"mypassword"}' | Select-Object -ExpandProperty Content
} catch {
  Write-Host $_.Exception.Response.StatusCode $_.Exception.Message
  Write-Host ([System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream()).ReadToEnd())
}
Write-Host "`n"

Write-Host "=== Test 5: Login with newly registered user ===" -ForegroundColor Green
Invoke-WebRequest -Uri "$BASE_URL/api/login" -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body '{"id":"john@example.com-Blynk","pass":"mypassword123"}' | Select-Object -ExpandProperty Content
Write-Host "`n"
