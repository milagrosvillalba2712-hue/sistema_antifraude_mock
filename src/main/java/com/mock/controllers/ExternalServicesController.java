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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            journal.record("IDENTIFICACIONES", documento,
                    (String) request.getAttribute("correlationId"), 200, 0, "INVALID_JSON");
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{invalid-json");
        }
        return execute("IDENTIFICACIONES", documento, request,
                new IdentidadResponse("Persona Sintética " + scenarios.normalizedScenario(documento),
                        "ACTIVO", scenarios.hasAntecedentes(documento)));
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

    @GetMapping("/clientes/{documento}/perfil")
    public ResponseEntity<?> clientProfile(@PathVariable String documento, HttpServletRequest request) {
        return execute("KYC_PERFIL", documento, request, profile(documento));
    }

    @GetMapping("/clientes/{documento}/documentos")
    public ResponseEntity<?> clientDocuments(@PathVariable String documento, HttpServletRequest request) {
        return execute("KYC_DOCUMENTOS", documento, request, documents(documento));
    }

    @GetMapping("/clientes/{documento}/historial-transaccional")
    public ResponseEntity<?> transactionHistory(@PathVariable String documento,
                                                @RequestParam(defaultValue = "15") int limit,
                                                HttpServletRequest request) {
        int boundedLimit = Math.max(1, Math.min(limit, 50));
        return execute("CORE_HISTORIAL", documento, request, history(documento, boundedLimit));
    }

    @GetMapping("/screening-listas/{documento}")
    public ResponseEntity<?> listScreening(@PathVariable String documento, HttpServletRequest request) {
        return execute("SCREENING_LISTAS", documento, request, Map.of(
                "documentoHashReferencia", "sha256-demo-" + scenarios.normalizedScenario(documento),
                "resultado", scenarios.findings(documento).contains("SIN_HALLAZGOS") ? "SIN_COINCIDENCIAS" : "CON_COINCIDENCIAS",
                "nivelRiesgo", scenarios.riskLevel(documento),
                "hallazgos", scenarios.findings(documento),
                "fuentes", List.of("ONU_DEMO", "OFAC_DEMO", "SEPRELAD_DEMO", "LISTA_INTERNA_DEMO"),
                "mensaje", "Datos sinteticos; no representan listas oficiales reales"
        ));
    }

    @GetMapping("/riesgo-pais/{codigoIso}")
    public ResponseEntity<?> countryRisk(@PathVariable String codigoIso, HttpServletRequest request) {
        return execute("RIESGO_PAIS", codigoIso, request, Map.of(
                "codigoIso", codigoIso.toUpperCase(),
                "categoria", isHighRiskCountry(codigoIso) ? "MONITOREO_INCREMENTADO_DEMO" : "ESTANDAR",
                "nivelRiesgo", isHighRiskCountry(codigoIso) ? "ALTO" : "BAJO",
                "fuentes", List.of("FATF_DEMO", "GAFILAT_DEMO"),
                "observacion", "Clasificacion sintetica para pruebas academicas"
        ));
    }

    @GetMapping("/beneficiario-final/{ruc}")
    public ResponseEntity<?> beneficialOwner(@PathVariable String ruc, HttpServletRequest request) {
        return execute("BENEFICIARIO_FINAL", ruc, request, Map.of(
                "rucHashReferencia", "sha256-demo-" + scenarios.normalizedScenario(ruc),
                "razonSocial", "Entidad Sintetica " + scenarios.normalizedScenario(ruc) + " S.A.",
                "beneficiarios", List.of(
                        Map.of("nombreCompleto", "Beneficiario Sintetico A", "participacion", 55, "pepRelacionado", scenarios.pep(ruc)),
                        Map.of("nombreCompleto", "Beneficiario Sintetico B", "participacion", 45, "pepRelacionado", false)
                ),
                "fuente", "Registro mercantil sintetico",
                "mensaje", "No usar como dato real"
        ));
    }

    @GetMapping("/proveedores/estado")
    public ResponseEntity<?> providersStatus(HttpServletRequest request) {
        return execute("ESTADO_PROVEEDORES", "100", request, Map.of(
                "identificaciones", "DISPONIBLE",
                "sanciones", "DISPONIBLE",
                "pep", "DISPONIBLE",
                "kycPerfil", "DISPONIBLE",
                "documentos", "DISPONIBLE",
                "historialTransaccional", "DISPONIBLE",
                "timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString()
        ));
    }

    @PostMapping("/licencias/validar")
    public ResponseEntity<?> validarLicencia(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String instalacionId = String.valueOf(body.getOrDefault("instalacionId", "desconocida"));
        String fingerprint = String.valueOf(body.getOrDefault("fingerprintHash", ""));
        return execute("LICENCIA_VALIDACION", instalacionId, request, Map.of(
                "instalacionIdHash", "sha256-demo-" + scenarios.normalizedScenario(instalacionId),
                "fingerprintMatch", fingerprint.startsWith("sha256-demo-") || fingerprint.length() >= 64,
                "estado", "VALIDO",
                "plan", "ESTANDAR",
                "vence", OffsetDateTime.now(ZoneOffset.UTC).plusDays(15).toString(),
                "mensaje", "Validacion de licencia simulada; no es una autorizacion criptografica real"
        ));
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

    private Map<String, Object> profile(String document) {
        String scenario = scenarios.normalizedScenario(document);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documentoHashReferencia", "sha256-demo-" + scenario);
        result.put("estadoApi", "DISPONIBLE_SIMULADO");
        result.put("nivelRiesgo", scenarios.riskLevel(document));
        result.put("personal", personalProfile(scenario));
        result.put("laboral", workProfile(scenario));
        result.put("academico", academicProfile(scenario));
        result.put("familiar", familyProfile(scenario));
        result.put("judicialRegulatorio", judicialProfile(document));
        return result;
    }

    private Map<String, Object> personalProfile(String scenario) {
        Map<String, Object> personal = new LinkedHashMap<>();
        personal.put("nombreCompleto", "Persona Sintetica " + scenario);
        personal.put("tipoDocumento", "CI_PY");
        personal.put("documentoEnmascarado", "***" + scenario);
        personal.put("fechaNacimiento", LocalDate.of(1988, 5, 20).toString());
        personal.put("fechaEmisionDocumento", LocalDate.of(2021, 3, 15).toString());
        personal.put("fechaExpiracionDocumento", LocalDate.of(2031, 3, 15).toString());
        personal.put("paisEmisionDocumento", "PY");
        personal.put("paisResidencia", "PY");
        personal.put("paisNacionalidad", "PY");
        personal.put("ciudadResidencia", "Asuncion");
        personal.put("departamentoResidencia", "Capital");
        personal.put("direccionResidencia", "Avda. Sintetica 1234, Barrio Demo");
        personal.put("telefonoFijoEnmascarado", "+595 21 *** " + scenario);
        personal.put("telefonoMovilEnmascarado", "+595 981 *** " + scenario);
        personal.put("email", "persona." + scenario + "@example.invalid");
        personal.put("edad", 38);
        personal.put("fotoDocumentoFrenteReferencia", "mock/ci-" + scenario + "-frente");
        personal.put("fotoDocumentoDorsoReferencia", "mock/ci-" + scenario + "-dorso");
        personal.put("fotoPerfilReferencia", "mock/perfil-" + scenario);
        return personal;
    }

    private Map<String, Object> workProfile(String scenario) {
        Map<String, Object> laboral = new LinkedHashMap<>();
        laboral.put("lugarTrabajo", "Servicios Sinteticos del Paraguay S.A.");
        laboral.put("direccionTrabajo", "Calle Laboral 456, Asuncion");
        laboral.put("contactoCorporativo", "rrhh." + scenario + "@empresa.example.invalid");
        laboral.put("ocupacion", "Servicios profesionales");
        laboral.put("rango", "Dependiente");
        laboral.put("antiguedad", "6 anos");
        laboral.put("ingresoMensualEstimado", 8500000);
        return laboral;
    }

    private Map<String, Object> academicProfile(String scenario) {
        Map<String, Object> academico = new LinkedHashMap<>();
        academico.put("nivelEstudios", "Universitario");
        academico.put("titulosObtenidos", List.of("Licenciatura Demo En Administracion"));
        academico.put("institucionEducativa", "Universidad Sintetica");
        academico.put("periodoCursada", "2010-2015");
        academico.put("calificacionesExpedientes", "No integrado; metadata disponible bajo solicitud");
        academico.put("logrosDestacados", List.of("Beca academica sintetica"));
        academico.put("certificacionesCursos", List.of("Cumplimiento Basico Demo", "Finanzas Personales Demo"));
        return academico;
    }

    private Map<String, Object> familyProfile(String scenario) {
        Map<String, Object> familiar = new LinkedHashMap<>();
        familiar.put("estadoCivil", "Casado");
        familiar.put("parentescosDirectos", List.of(
                Map.of("nombre", "Familiar Sintetico A", "parentesco", "Conyuge", "edad", 37, "ocupacion", "Docencia"),
                Map.of("nombre", "Familiar Sintetico B", "parentesco", "Hijo/a", "edad", 9, "ocupacion", "Estudiante")
        ));
        familiar.put("contactoEmergencia", "+595 981 *** " + scenario);
        familiar.put("direccionContactoEmergencia", "Avda. Familiar 789, Asuncion");
        return familiar;
    }

    private Map<String, Object> judicialProfile(String document) {
        Map<String, Object> judicial = new LinkedHashMap<>();
        judicial.put("antecedentesPenales", scenarios.hasAntecedentes(document));
        judicial.put("procesosJudicialesActivos", scenarios.hasAntecedentes(document) ? "Revision sintetica requerida" : "Sin procesos simulados");
        judicial.put("ordenesRequerimientos", "Sin ordenes simuladas vigentes");
        judicial.put("historialLitigios", "Sin litigios simulados relevantes");
        judicial.put("pep", scenarios.pep(document));
        judicial.put("sancionado", scenarios.sanctioned(document));
        judicial.put("nivelRiesgo", scenarios.riskLevel(document));
        judicial.put("hallazgos", scenarios.findings(document));
        return judicial;
    }

    private Map<String, Object> documents(String document) {
        String scenario = scenarios.normalizedScenario(document);
        return Map.of(
                "documentoHashReferencia", "sha256-demo-" + scenario,
                "documentos", List.of(
                        Map.of("tipo", "CI_PY", "estado", "VIGENTE", "frenteDisponible", true, "dorsoDisponible", true, "referencia", "mock/ci-" + scenario),
                        Map.of("tipo", "COMPROBANTE_DOMICILIO", "estado", "PENDIENTE_VALIDACION", "referencia", "mock/domicilio-" + scenario)
                ),
                "mensaje", "Solo metadata sintetica; no contiene imagenes ni archivos reales"
        );
    }

    private Map<String, Object> history(String document, int limit) {
        String scenario = scenarios.normalizedScenario(document);
        List<Map<String, Object>> transactions = java.util.stream.IntStream.rangeClosed(1, limit)
                .mapToObj(index -> Map.<String, Object>of(
                        "codigo", "EXT-TX-" + scenario + "-" + String.format("%03d", index),
                        "fecha", OffsetDateTime.of(2026, 8, Math.min(index, 28), 9, 30, 0, 0, ZoneOffset.of("-03:00")).toString(),
                        "tipo", switch (index % 5) {
                            case 0 -> "PY_SPI_ALIAS_TRANSFER";
                            case 1 -> "PY_CASH_IN_BRANCH";
                            case 2 -> "PY_EMPE_WALLET_P2P";
                            case 3 -> "PY_QR_EMV_PAYMENT";
                            default -> "PY_REMITTANCE_SEND";
                        },
                        "monto", BigDecimal.valueOf(750_000L + (long) index * 425_000L),
                        "moneda", index % 4 == 0 ? "USD" : "PYG",
                        "estado", "COMPLETADA",
                        "scoreRiesgo", index % 5 == 0 ? 85 : 25 + index
                ))
                .toList();
        return Map.of(
                "documentoHashReferencia", "sha256-demo-" + scenario,
                "cantidad", limit,
                "transacciones", transactions
        );
    }

    private boolean isHighRiskCountry(String codigoIso) {
        String normalized = codigoIso == null ? "" : codigoIso.toUpperCase();
        return java.util.Set.of("HK", "AE", "PA", "ZA").contains(normalized);
    }
}
