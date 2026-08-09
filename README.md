# Regula Academic External API Mock

Servicio independiente para demostrar que Regula consume APIs externas mediante HTTPS sin utilizar datos reales.

## Contrato

La fuente de verdad es `openapi/regula-external-services-v1.yaml`:

- `GET /api/v1/identidades/{documento}`
- `GET /api/v1/sanciones/{documento}`
- `GET /api/v1/personas-expuestas/{documento}`
- `GET /api/v1/clientes/{documento}/perfil`
- `GET /api/v1/clientes/{documento}/documentos`
- `GET /api/v1/clientes/{documento}/historial-transaccional`
- `GET /api/v1/screening-listas/{documento}`
- `GET /api/v1/riesgo-pais/{codigoIso}`
- `GET /api/v1/beneficiario-final/{ruc}`
- `GET /api/v1/proveedores/estado`

Todos requieren `X-API-Key`; el endpoint `/admin/v1/auditoria` exige una credencial administrativa distinta. `X-Correlation-Id` se conserva o se genera.

El analisis funcional de APIs esperadas esta en `docs/APIS_EXTERNAS_ESPERADAS_REGULA.md`.

## Escenarios

| Documento sintético | Resultado |
|---|---|
| `100` | Normal |
| `200` | Sancionado |
| `300` | PEP |
| `400` | Sancionado y PEP |
| `not-found` | 404 |
| `rate-limit` | 429 |
| `server-error` | 500 |
| `unavailable` | 503 |
| `timeout` | Demora de 7 segundos |
| `invalid-json` | JSON inválido |

## TLS

Generar una CA académica, el keystore y el truststore del backend:

```powershell
powershell -ExecutionPolicy Bypass -File .\generate-ssl.ps1 `
  -KeyStorePassword '<secreto>' -TrustStorePassword '<secreto-distinto>'
```

No se versionan claves privadas ni contraseñas. Para ejecutar, copiar `.env.example` a `.env`, reemplazar todos los valores y usar `docker compose up --build`.

## Checks

```powershell
mvn clean test
java scripts/TlsProbe.java certificates/academic-ca.crt
```

El segundo comando se ejecuta con el mock HTTPS levantado. Debe aceptar la CA académica y rechazar tanto una CA no confiable como un hostname incorrecto.
