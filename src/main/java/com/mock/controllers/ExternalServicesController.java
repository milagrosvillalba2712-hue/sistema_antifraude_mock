package com.mock.controllers;

import com.mock.dto.IdentidadResponse;
import com.mock.dto.PepResponse;
import com.mock.dto.SancionadoResponse;
import com.mock.services.AuditJournal;
import com.mock.services.ScenarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ExternalServicesController {
    private final ScenarioService scenarios;
    private final AuditJournal journal;

    public ExternalServicesController(ScenarioService scenarios, AuditJournal journal) {
        this.scenarios = scenarios;
        this.journal = journal;
    }

    @GetMapping("/identidades/{documento}")
    public ResponseEntity<?> identity(@PathVariable String documento, HttpServletRequest request) {
        if ("invalid-json".equals(documento)) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{invalid-json");
        }
        return execute("IDENTIFICACIONES", documento, request,
                new IdentidadResponse("Persona Sintética", "ACTIVO", false));
    }

    @GetMapping("/sanciones/{documento}")
    public ResponseEntity<?> sanctions(@PathVariable String documento, HttpServletRequest request) {
        return execute("BCP", documento, request,
                new SancionadoResponse(scenarios.sanctioned(documento), "BCP_SIMULADO"));
    }

    @GetMapping("/personas-expuestas/{documento}")
    public ResponseEntity<?> pep(@PathVariable String documento, HttpServletRequest request) {
        boolean pep = scenarios.pep(documento);
        return execute("SEPRELAD", documento, request,
                new PepResponse(pep, pep ? "ALTO" : "NINGUNO"));
    }

    private ResponseEntity<?> execute(String provider, String document, HttpServletRequest request, Object response) {
        long start = System.nanoTime();
        String correlation = (String) request.getAttribute("correlationId");
        try {
            scenarios.apply(document);
            journal.record(provider, document, correlation, 200, elapsed(start), "OK");
            return ResponseEntity.ok(response);
        } catch (RuntimeException exception) {
            int status = exception instanceof org.springframework.web.server.ResponseStatusException statusException
                    ? statusException.getStatusCode().value() : 500;
            journal.record(provider, document, correlation, status, elapsed(start), "ERROR");
            throw exception;
        }
    }

    private long elapsed(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
