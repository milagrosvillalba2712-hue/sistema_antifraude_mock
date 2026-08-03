# AGENTS.md — Regula external API mock

Spring Boot 3.2.5 / Java 17 service used only for synthetic academic demonstrations.

## Commands

```powershell
mvn clean test
powershell -ExecutionPolicy Bypass -File .\generate-ssl.ps1 -KeyStorePassword '<secret>' -TrustStorePassword '<different-secret>'
docker compose up --build
```

## Contract

`openapi/regula-external-services-v1.yaml` is the source of truth. Public simulated endpoints live under `/api/v1`; sanitized administration lives under `/admin/v1`.

## Security invariants

- Never use real people, documents, sanctions or PEP records.
- Never log or persist a plaintext document, API Key, private key or complete provider response.
- Operational and administrative API Keys must differ and have no code default.
- TLS uses the generated academic CA, explicit SAN and an external keystore.
- Acceptance forbids `trustAll`, hostname-verification bypass and insecure curl flags.
- The backend trusts only `regula-external-truststore.p12` for this provider.
- Generated certificates and `.env` are local artifacts and must remain ignored.

## Scenarios

Use identifiers `100`, `200`, `300`, `400`, `not-found`, `rate-limit`, `server-error`, `unavailable`, `timeout` and `invalid-json`. They are scenario selectors, not identity data.
