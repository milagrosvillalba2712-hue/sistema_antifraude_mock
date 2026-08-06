param(
    [Parameter(Mandatory = $true)][string]$KeyStorePassword,
    [Parameter(Mandatory = $true)][string]$TrustStorePassword
)

$ErrorActionPreference = 'Stop'
if ($KeyStorePassword -match '[\$\s]') { throw 'KeyStorePassword no puede contener $ ni espacios para evitar interpolacion de Compose.' }
if ($TrustStorePassword -match '[\$\s]') { throw 'TrustStorePassword no puede contener $ ni espacios.' }
if ($KeyStorePassword -eq $TrustStorePassword) { throw 'Las contrasenas de keystore y truststore deben ser diferentes.' }
$certDir = Join-Path $PSScriptRoot 'certificates'
$backendCertDir = Join-Path (Split-Path $PSScriptRoot -Parent) 'sistema_antifraude_backend\certificates'
New-Item -ItemType Directory -Force -Path $certDir, $backendCertDir | Out-Null

$caStore = Join-Path $certDir 'academic-ca.p12'
$caCert = Join-Path $certDir 'academic-ca.crt'
$serverStore = Join-Path $certDir 'mock-api.p12'
$request = Join-Path $certDir 'mock-api.csr'
$signedCert = Join-Path $certDir 'mock-api.crt'
$trustStore = Join-Path $backendCertDir 'regula-external-truststore.p12'

foreach ($file in @($caStore, $caCert, $serverStore, $request, $signedCert, $trustStore)) {
    if (Test-Path -LiteralPath $file) { Remove-Item -LiteralPath $file -Force }
}

keytool -genkeypair -alias academic-ca -keyalg RSA -keysize 3072 -validity 3650 `
    -dname 'CN=Regula Academic CA,O=Regula Thesis,C=PY' -ext bc:c `
    -storetype PKCS12 -keystore $caStore -storepass $KeyStorePassword -keypass $KeyStorePassword -noprompt
keytool -exportcert -rfc -alias academic-ca -keystore $caStore -storepass $KeyStorePassword -file $caCert

keytool -genkeypair -alias mock-api -keyalg RSA -keysize 3072 -validity 825 `
    -dname 'CN=mock-api,O=Regula Thesis,C=PY' `
    -ext 'SAN=dns:mock-api,dns:localhost,ip:127.0.0.1' `
    -storetype PKCS12 -keystore $serverStore -storepass $KeyStorePassword -keypass $KeyStorePassword -noprompt
keytool -certreq -alias mock-api -keystore $serverStore -storepass $KeyStorePassword -file $request `
    -ext 'SAN=dns:mock-api,dns:localhost,ip:127.0.0.1'
keytool -gencert -rfc -alias academic-ca -keystore $caStore -storepass $KeyStorePassword `
    -infile $request -outfile $signedCert -validity 825 `
    -ext 'SAN=dns:mock-api,dns:localhost,ip:127.0.0.1' -ext KU=digitalSignature,keyEncipherment -ext EKU=serverAuth
keytool -importcert -alias academic-ca -keystore $serverStore -storepass $KeyStorePassword -file $caCert -noprompt
keytool -importcert -alias mock-api -keystore $serverStore -storepass $KeyStorePassword -file $signedCert -noprompt
keytool -importcert -alias academic-ca -storetype PKCS12 -keystore $trustStore `
    -storepass $TrustStorePassword -file $caCert -noprompt

keytool -list -keystore $serverStore -storepass $KeyStorePassword -alias mock-api | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'No se pudo validar el keystore generado.' }
keytool -list -keystore $trustStore -storepass $TrustStorePassword -alias academic-ca | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'No se pudo validar el truststore generado.' }

Remove-Item -LiteralPath $caStore, $request, $signedCert -Force
Write-Output "CA=$caCert"
Write-Output "KEYSTORE=$serverStore"
Write-Output "TRUSTSTORE=$trustStore"
