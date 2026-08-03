package com.mock.exceptions;

import com.mock.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<java.util.Map<String, Object>> handleStatus(ResponseStatusException ex,
                                                                       HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatusCode()).body(java.util.Map.of(
                "codigo", "MOCK_" + ex.getStatusCode().value(),
                "mensaje", ex.getReason() == null ? "Error simulado" : ex.getReason(),
                "correlationId", String.valueOf(request.getAttribute("correlationId"))));
    }

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Error no controlado de tipo {}", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .codigo(500)
                        .mensaje("Internal Server Error")
                        .detalle("Error interno simulado")
                        .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                        .codigo(400)
                        .mensaje("Bad Request")
                        .detalle(ex.getMessage())
                        .build());
    }
}
