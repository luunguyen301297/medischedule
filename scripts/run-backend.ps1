# Chạy backend Spring Boot ở chế độ dev: nạp biến từ .env rồi gọi mvnw spring-boot:run.
# Dùng bởi VS Code (launch "Backend (mvnw)") hoặc chạy tay: powershell -File scripts\run-backend.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

$envFile = Join-Path $root '.env'
if (-not (Test-Path $envFile)) { throw "Thiếu file .env, hãy copy từ .env.example" }
Get-Content $envFile -Encoding UTF8 | Where-Object { $_ -match '^\s*[A-Za-z_][A-Za-z0-9_]*=' } | ForEach-Object {
    $k, $v = $_ -split '=', 2
    Set-Item -Path "env:$($k.Trim())" -Value $v
}

if (-not $env:JAVA_HOME) { $env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine') }
if ($env:JAVA_HOME) { $env:Path = "$env:JAVA_HOME\bin;$env:Path" }

Set-Location (Join-Path $root 'backend')
& .\mvnw.cmd spring-boot:run
exit $LASTEXITCODE
