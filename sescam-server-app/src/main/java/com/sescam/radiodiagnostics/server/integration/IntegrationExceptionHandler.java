package com.sescam.radiodiagnostics.server.integration;

import com.sescam.radiodiagnostics.common.integration.ConcurrentIntegrationChangeException;
import com.sescam.radiodiagnostics.common.integration.IntegrationConnectionFailedException;
import com.sescam.radiodiagnostics.common.integration.IntegrationNotFoundException;
import com.sescam.radiodiagnostics.common.integration.IntegrationSaveException;
import com.sescam.radiodiagnostics.common.integration.InvalidIntegrationSettingsException;
import com.sescam.radiodiagnostics.common.integration.UnauthorizedIntegrationAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps the UC-ADM-04 exceptions to HTTP errors. Limited to {@link IntegrationRestController}.
 * No message contains settings or credentials (A4).
 */
@RestControllerAdvice(assignableTypes = IntegrationRestController.class)
public class IntegrationExceptionHandler {

    @ExceptionHandler(UnauthorizedIntegrationAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedIntegrationAccessException ex) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage());
    }

    @ExceptionHandler(IntegrationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(IntegrationNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InvalidIntegrationSettingsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalid(InvalidIntegrationSettingsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "INVALID_SETTINGS", "message", ex.getMessage(), "errors", ex.getErrors()));
    }

    @ExceptionHandler(IntegrationConnectionFailedException.class)
    public ResponseEntity<Map<String, Object>> handleConnectionFailed(IntegrationConnectionFailedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "error", "CONNECTION_FAILED", "status", ex.getStatus().name(), "message", ex.getMessage()));
    }

    @ExceptionHandler(ConcurrentIntegrationChangeException.class)
    public ResponseEntity<Map<String, Object>> handleConcurrent(ConcurrentIntegrationChangeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "CONCURRENT_MODIFICATION",
                "expectedVersion", ex.getExpectedVersion(),
                "actualVersion", ex.getActualVersion(),
                "message", ex.getMessage()));
    }

    @ExceptionHandler(IntegrationSaveException.class)
    public ResponseEntity<Map<String, Object>> handleSaveFailure(IntegrationSaveException ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "SAVE_FAILED", ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        // the original message could quote the request body, so it is not returned
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "The request body is not valid.");
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("error", code, "message", message));
    }
}
