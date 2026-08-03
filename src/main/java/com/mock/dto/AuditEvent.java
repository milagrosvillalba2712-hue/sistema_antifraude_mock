package com.mock.dto;

import java.time.OffsetDateTime;

public record AuditEvent(long id, String proveedor, String documentoHash, String correlationId,
                         int statusHttp, long duracionMs, String resultado, OffsetDateTime fecha) {
}
