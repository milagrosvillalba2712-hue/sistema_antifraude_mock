param(
    [Parameter(Mandatory = $true)][string]$KeyStorePassword,
    [Parameter(Mandatory = $true)][string]$TrustStorePassword
)

$ErrorActionPreference = 'Stop'
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

Remove-Item -LiteralPath $caStore, $request, $signedCert -Force
Write-Output "CA=$caCert"
Write-Output "KEYSTORE=$serverStore"
Write-Output "TRUSTSTORE=$trustStore"
