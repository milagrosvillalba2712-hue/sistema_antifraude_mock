# Gate de Fase 3 — integración externa académica

## Contrato y escenarios

- OpenAPI 3.0.3 versionado para Identificaciones, BCP/Sanciones y SEPRELAD/PEP.
- Escenarios deterministas: normal (`100`), sancionado (`200`), PEP (`300`), combinado (`400`), no encontrado, timeout, 429, 500, 503 y JSON inválido.
- Ningún escenario representa datos reales.

## Seguridad

- TLS con CA académica privada y SAN para `mock-api`, `localhost` y `127.0.0.1`.
- Keystore externo al JAR y truststore exclusivo en backend.
- Credenciales operativa y administrativa separadas.
- Sin H2 Console, CORS abierto, `trustAll`, `curl -k` ni hostname verification deshabilitado.
- Auditoría almacena SHA-256 del documento; nunca documento plano, API Key o respuesta completa.

## Backend

- Clientes tipados `IdentificacionesClient`, `BcpSancionesClient` y `SepreladPepClient`.
- Timeout total inferior a cinco segundos con tres intentos, backoff exponencial y jitter.
- 4xx no se reintenta; errores transitorios sí.
- Circuit breaker, bulkhead, métricas Actuator y correlation ID.
- JWT interno no se propaga.
- Flyway V6 sanitiza `consultas_externas`.

## Aceptación

Ejecutar `mvn test` en mock y backend con Java 17. Ejecutar `generate-ssl.ps1` y verificar que la CA académica acepta el endpoint y el truststore del sistema lo rechaza.
