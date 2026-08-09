package com.mock.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ScenarioService {
    public static final java.util.Set<String> KNOWN_SCENARIOS = java.util.Set.of(
            "100", "200", "300", "400", "not-found", "rate-limit", "server-error",
            "unavailable", "timeout", "invalid-json"
    );

    public void apply(String document) {
        try {
            switch (document) {
                case "timeout" -> Thread.sleep(7_000);
                case "not-found" -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado");
                case "rate-limit" -> throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Límite simulado");
                case "server-error" -> throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error simulado");
                case "unavailable" -> throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio no disponible");
                default -> { }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Interrumpido");
        }
    }

    public boolean sanctioned(String document) {
        return document.equals("200") || document.equals("400");
    }

    public boolean pep(String document) {
        return document.equals("300") || document.equals("400");
    }

    public boolean hasAntecedentes(String document) {
        return document.equals("200") || document.equals("400");
    }

    public String riskLevel(String document) {
        if (document.equals("400")) return "CRITICO";
        if (document.equals("200") || document.equals("300")) return "ALTO";
        return "BAJO";
    }

    public java.util.List<String> findings(String document) {
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        if (sanctioned(document)) result.add("COINCIDENCIA_LISTA_SANCIONES_DEMO");
        if (pep(document)) result.add("PEP_DEMO");
        if (hasAntecedentes(document)) result.add("ANTECEDENTE_JUDICIAL_DEMO");
        if (result.isEmpty()) result.add("SIN_HALLAZGOS");
        return result;
    }

    public String normalizedScenario(String value) {
        if (value == null || value.isBlank()) return "100";
        return KNOWN_SCENARIOS.contains(value) ? value : "100";
    }
}
