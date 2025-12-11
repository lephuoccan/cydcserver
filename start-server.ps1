# Start CYDConnect Server
Write-Host "Starting CYDConnect Server..." -ForegroundColor Cyan
Start-Process -FilePath "java" -ArgumentList "-jar","target/cydcserver-1.0.0.jar" -WindowStyle Normal -PassThru
Write-Host "Server started in new window!" -ForegroundColor Green
