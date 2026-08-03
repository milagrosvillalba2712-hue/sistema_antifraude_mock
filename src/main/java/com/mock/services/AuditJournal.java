package com.mock.services;

import com.mock.dto.AuditEvent;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AuditJournal {
    private final AtomicLong sequence = new AtomicLong();
    private final List<AuditEvent> events = new CopyOnWriteArrayList<>();
    private final java.nio.file.Path durablePath;
    private final ObjectMapper objectMapper;

    public AuditJournal(@Value("${mock.audit.path:}") String path, ObjectMapper objectMapper) {
        this.durablePath = path.isBlank() ? null : java.nio.file.Path.of(path);
        this.objectMapper = objectMapper;
    }

    public void record(String provider, String document, String correlationId, int status, long duration, String result) {
        AuditEvent event = new AuditEvent(sequence.incrementAndGet(), provider, hash(document), correlationId,
                status, duration, result, OffsetDateTime.now());
        events.add(event);
        append(event);
    }

    public List<AuditEvent> events() {
        return List.copyOf(events);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo sanitizar el documento", exception);
        }
    }

    private synchronized void append(AuditEvent event) {
        if (durablePath == null) return;
        try {
            java.nio.file.Files.createDirectories(durablePath.getParent());
            java.nio.file.Files.writeString(durablePath, objectMapper.writeValueAsString(event) + System.lineSeparator(),
                    StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo persistir el journal sanitizado", exception);
        }
    }
}
