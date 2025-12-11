$response = Invoke-WebRequest `
    -Uri "http://192.168.1.9:8081/api/pins" `
    -Method PUT `
    -Headers @{
        "Authorization" = "esp32@cydc.local-Blynk-0-0-4ba7c726a0a54cb390065907068d9922"
        "Content-Type" = "application/json"
    } `
    -Body '{"pin":"V5","value":"999"}'

Write-Host "Status Code: $($response.StatusCode)"
Write-Host "Content: $($response.Content)"
