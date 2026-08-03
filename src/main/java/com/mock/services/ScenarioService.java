package com.mock.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ScenarioService {
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
}
