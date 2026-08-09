# APIs Externas Esperadas Por Regula AML

Este mock existe solo para demostracion academica. No contiene personas reales, documentos reales, sanciones reales, PEP reales ni respuestas productivas de proveedores.

## Contratos Consumidos Actualmente Por Backend

| Necesidad | Endpoint mock | Uso esperado |
|---|---|---|
| Identificacion civil/KYC basico | `GET /api/v1/identidades/{documento}` | Validar existencia/estado documental y antecedentes sinteticos. |
| Screening de sanciones | `GET /api/v1/sanciones/{documento}` | Simular coincidencias de sanciones/listas regulatorias. |
| Screening PEP | `GET /api/v1/personas-expuestas/{documento}` | Simular persona expuesta politicamente o relacionada. |

## Contratos Preparados Para El Flujo De Alertas

| Necesidad | Endpoint mock | Uso esperado |
|---|---|---|
| Perfil KYC enriquecido | `GET /api/v1/clientes/{documento}/perfil` | Mostrar detalle de cliente en alerta: personal, laboral, academico, familiar y judicial/regulatorio. |
| Metadata documental | `GET /api/v1/clientes/{documento}/documentos` | Simular foto/archivo disponible sin servir imagenes reales. |
| Historial transaccional externo | `GET /api/v1/clientes/{documento}/historial-transaccional?limit=15` | Mostrar ultimas operaciones del cliente durante investigacion. |
| Screening consolidado | `GET /api/v1/screening-listas/{documento}` | Devolver hallazgos de listas, PEP, sanciones y observados en una respuesta unica. |
| Riesgo pais | `GET /api/v1/riesgo-pais/{codigoIso}` | Probar reglas por pais origen/destino o corredores internacionales. |
| Beneficiario final | `GET /api/v1/beneficiario-final/{ruc}` | Simular estructura de beneficiarios de persona juridica. |
| Estado de proveedores | `GET /api/v1/proveedores/estado` | Validar disponibilidad operativa de proveedores externos. |

## Escenarios Sinteticos

Los valores de `{documento}` o `{ruc}` son selectores de escenario:

| Selector | Resultado |
|---|---|
| `100` | Cliente normal. |
| `200` | Antecedente/sancion demo. |
| `300` | PEP demo. |
| `400` | PEP + sancion + antecedente demo. |
| `not-found` | 404. |
| `rate-limit` | 429. |
| `server-error` | 500. |
| `unavailable` | 503. |
| `timeout` | Demora simulada de 7 segundos. |
| `invalid-json` | JSON invalido solo en identidad. |

## Requisitos Pendientes En Backend

- Crear clientes especificos para los endpoints enriquecidos.
- Registrar cada consulta en `consultas_externas` con correlation id, estado HTTP, duracion, proveedor y hash de documento.
- No persistir payload completo de proveedor; guardar snapshot sanitizado solo cuando el flujo de alerta lo requiera.
- Usar `X-Correlation-Id` de punta a punta.
- Mantener `X-API-Key` operacional separada de la administrativa.
- Implementar fallback visual en frontend cuando el mock responda `503`, `429` o timeout.
