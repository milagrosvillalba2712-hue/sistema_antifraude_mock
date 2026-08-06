param(
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$repo = $PSScriptRoot
$envFile = Join-Path $repo '.env'
$keystore = Join-Path $repo 'certificates\mock-api.p12'
$ca = Join-Path $repo 'certificates\academic-ca.crt'

if (-not (Test-Path -LiteralPath $envFile)) { throw 'Falta .env. Copie .env.example y genere secretos nuevos.' }
$values = @{}
Get-Content -LiteralPath $envFile | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { $values[$Matches[1].Trim()] = $Matches[2].Trim() }
}
foreach ($name in 'MOCK_OPERATIONAL_API_KEY','MOCK_ADMIN_API_KEY','SSL_KEYSTORE_PASSWORD') {
    if ([string]::IsNullOrWhiteSpace($values[$name]) -or $values[$name] -match '^replace_') { throw "Falta un valor real para $name" }
    if ($values[$name] -match '[\$\s]') { throw "$name contiene caracteres no seguros para Compose" }
}
if ($values.MOCK_OPERATIONAL_API_KEY -eq $values.MOCK_ADMIN_API_KEY) { throw 'Las API Keys operativa y administrativa deben ser diferentes.' }
if (-not (Test-Path -LiteralPath $keystore) -or -not (Test-Path -LiteralPath $ca)) { throw 'Faltan certificados generados.' }

$previousErrorPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$javaVersion = (& java -version 2>&1 | Select-Object -First 1)
$ErrorActionPreference = $previousErrorPreference
if ($javaVersion -notmatch 'version "17\.') { throw "Java 17 es obligatorio. Detectado: $javaVersion" }
keytool -list -keystore $keystore -storepass $values.SSL_KEYSTORE_PASSWORD -alias mock-api | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'La contrasena de .env no abre certificates/mock-api.p12.' }

Push-Location $repo
try {
    if (-not $SkipBuild) {
        mvn clean test
        if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas Maven.' }
        docker compose build --no-cache mock-api
        if ($LASTEXITCODE -ne 0) { throw 'Fallo el build Docker sin cache.' }
    }
    docker compose up -d --force-recreate
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar el mock.' }
    $healthy = $false
    for ($attempt = 1; $attempt -le 30; $attempt++) {
        $containerId = docker compose ps -q mock-api
        if ($containerId) {
            $status = docker inspect --format '{{.State.Health.Status}}' $containerId
            if ($status -eq 'healthy') { $healthy = $true; break }
            if ($status -eq 'unhealthy') { docker compose logs --tail 80 mock-api; throw 'Mock unhealthy.' }
        }
        Start-Sleep -Seconds 2
    }
    if (-not $healthy) { throw 'El mock no alcanzo estado healthy dentro de 60 segundos.' }
    java scripts\TlsProbe.java certificates\academic-ca.crt
    if ($LASTEXITCODE -ne 0) { throw 'Fallo el TLS probe.' }
    Write-Output 'MOCK_GATE=PASS'
} finally {
    Pop-Location
}
