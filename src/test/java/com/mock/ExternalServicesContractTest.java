package com.mock;

import com.mock.services.AuditJournal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "server.ssl.enabled=false",
        "mock.api.operational-key=operational-test-key",
        "mock.api.admin-key=admin-test-key"
})
@AutoConfigureMockMvc
class ExternalServicesContractTest {
    @Autowired MockMvc mvc;
    @Autowired AuditJournal journal;

    @Test
    void openApiEsValidoYContieneLosTresContratos() {
        var result = new io.swagger.v3.parser.OpenAPIV3Parser()
                .readLocation(java.nio.file.Path.of("openapi/regula-external-services-v1.yaml")
                        .toAbsolutePath().toUri().toString(), null, null);
        assertThat(result.getMessages()).isEmpty();
        assertThat(result.getOpenAPI().getPaths()).containsKeys(
                "/api/v1/identidades/{documento}",
                "/api/v1/sanciones/{documento}",
                "/api/v1/personas-expuestas/{documento}",
                "/api/v1/clientes/{documento}/perfil",
                "/api/v1/clientes/{documento}/documentos",
                "/api/v1/clientes/{documento}/historial-transaccional",
                "/api/v1/screening-listas/{documento}",
                "/api/v1/riesgo-pais/{codigoIso}",
                "/api/v1/beneficiario-final/{ruc}",
                "/api/v1/proveedores/estado");
    }

    @Test
    void apiKeyEsObligatoria() throws Exception {
        mvc.perform(get("/api/v1/identidades/100"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void contratosTipadosYCorrelationId() throws Exception {
        mvc.perform(get("/api/v1/identidades/100")
                        .header("X-API-Key", "operational-test-key")
                        .header("X-Correlation-Id", "11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.nombreCompleto").value("Persona Sintética 100"));
        mvc.perform(get("/api/v1/sanciones/200").header("X-API-Key", "operational-test-key"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sancionado").value(true));
        mvc.perform(get("/api/v1/personas-expuestas/300").header("X-API-Key", "operational-test-key"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pep").value(true));
    }

    @Test
    void escenariosTransitoriosYNoEncontrado() throws Exception {
        mvc.perform(get("/api/v1/sanciones/rate-limit").header("X-API-Key", "operational-test-key"))
                .andExpect(status().isTooManyRequests());
        mvc.perform(get("/api/v1/sanciones/unavailable").header("X-API-Key", "operational-test-key"))
                .andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/v1/sanciones/not-found").header("X-API-Key", "operational-test-key"))
                .andExpect(status().isNotFound());
    }

    @Test
    void contratosKycEnriquecidosParaDetalleDeAlerta() throws Exception {
        mvc.perform(get("/api/v1/clientes/400/perfil").header("X-API-Key", "operational-test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivelRiesgo").value("CRITICO"))
                .andExpect(jsonPath("$.judicialRegulatorio.pep").value(true))
                .andExpect(jsonPath("$.judicialRegulatorio.sancionado").value(true));

        mvc.perform(get("/api/v1/clientes/100/historial-transaccional?limit=15").header("X-API-Key", "operational-test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(15))
                .andExpect(jsonPath("$.transacciones.length()").value(15));

        mvc.perform(get("/api/v1/screening-listas/200").header("X-API-Key", "operational-test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("CON_COINCIDENCIAS"));

        mvc.perform(get("/api/v1/riesgo-pais/HK").header("X-API-Key", "operational-test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivelRiesgo").value("ALTO"));
    }

    @Test
    void credencialAdministrativaEstaSeparadaYAuditoriaSanitizada() throws Exception {
        mvc.perform(get("/api/v1/sanciones/200").header("X-API-Key", "operational-test-key"))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/v1/auditoria").header("X-API-Key", "operational-test-key"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/v1/auditoria").header("X-API-Key", "admin-test-key"))
                .andExpect(status().isOk());
        assertThat(journal.events()).isNotEmpty();
        assertThat(journal.events().get(0).documentoHash()).hasSize(64).doesNotContain("200");
    }

    @Test
    void journalDurableNoContieneDocumentoNiApiKey(@TempDir java.nio.file.Path directory) throws Exception {
        var file = directory.resolve("events.jsonl");
        var durable = new AuditJournal(file.toString(), new com.fasterxml.jackson.databind.ObjectMapper()
                .findAndRegisterModules());
        durable.record("BCP", "documento-super-secreto", "correlation-safe", 200, 12, "OK");
        String content = java.nio.file.Files.readString(file);
        assertThat(content).doesNotContain("documento-super-secreto", "api-key")
                .contains("correlation-safe");
    }
}
