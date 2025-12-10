$token = "apitest@example.com-IoTApp-100-1-00eab6c272d241659af31c7b48092bce"
$uri = "http://localhost:8081/api/pin/1/V0"

Write-Host "Testing token validation..." -ForegroundColor Yellow
Write-Host "Token: $token" -ForegroundColor Gray

# Test 1: GET without token
Write-Host "`nTest 1: GET without token"
try {
    $response = Invoke-WebRequest -Uri $uri -Method GET -UseBasicParsing
    Write-Host "Success: $($response.Content)"
} catch {
    Write-Host "Error: $($_.Exception.Response.StatusCode)"
}

# Test 2: GET with token in query
Write-Host "`nTest 2: GET with token in query"
$uriWithToken = "$uri`?token=$token"
Write-Host "URL: $uriWithToken" -ForegroundColor Gray
try {
    $response = Invoke-WebRequest -Uri $uriWithToken -Method GET -UseBasicParsing
    Write-Host "Success: $($response.Content)"
} catch {
    $ex = $_
    Write-Host "Error: $($ex.Exception.Response.StatusCode)"
    try {
        $reader = New-Object System.IO.StreamReader($ex.Exception.Response.GetResponseStream())
        $body = $reader.ReadToEnd()
        Write-Host "Body: $body"
    } catch {}
}

# Test 3: PUT with token in body
Write-Host "`nTest 3: PUT with token in JSON body"
$body = @{ value = "42"; token = $token } | ConvertTo-Json
Write-Host "Body: $body" -ForegroundColor Gray
try {
    $response = Invoke-WebRequest -Uri $uri -Method PUT -Headers @{"Content-Type"="application/json"} -Body $body -UseBasicParsing
    Write-Host "Success: $($response.Content)"
} catch {
    $ex = $_
    Write-Host "Error: $($ex.Exception.Response.StatusCode)"
    try {
        $reader = New-Object System.IO.StreamReader($ex.Exception.Response.GetResponseStream())
        $body = $reader.ReadToEnd()
        Write-Host "Body: $body"
    } catch {}
}
