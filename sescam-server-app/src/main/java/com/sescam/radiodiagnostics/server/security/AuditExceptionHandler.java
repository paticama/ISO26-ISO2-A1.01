package com.sescam.radiodiagnostics.server.security;

import com.sescam.radiodiagnostics.common.security.AuditQueryFailedException;
import com.sescam.radiodiagnostics.common.security.AuditQueryTooBroadException;
import com.sescam.radiodiagnostics.common.security.InvalidAuditQueryException;
import com.sescam.radiodiagnostics.common.security.UnauthorizedAuditAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/**
 * Maps the UC-ADM-03 exceptions to HTTP errors. Limited to {@link AuditRestController}.
 */
@RestControllerAdvice(assignableTypes = AuditRestController.class)
public class AuditExceptionHandler {

    @ExceptionHandler(UnauthorizedAuditAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedAuditAccessException ex) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage());
    }

    @ExceptionHandler(InvalidAuditQueryException.class)
    public ResponseEntity<Map<String, Object>> handleInvalid(InvalidAuditQueryException ex) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_QUERY", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_QUERY",
                "Invalid value for '" + ex.getName() + "'. Use ISO-8601, e.g. 2026-11-20T00:00:00Z.");
    }

    @ExceptionHandler(AuditQueryTooBroadException.class)
    public ResponseEntity<Map<String, Object>> handleTooBroad(AuditQueryTooBroadException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                "error", "QUERY_TOO_BROAD", "message", ex.getMessage(),
                "limit", ex.getLimit(), "actual", ex.getActual()));
    }

    @ExceptionHandler(AuditQueryFailedException.class)
    public ResponseEntity<Map<String, Object>> handleFailure(AuditQueryFailedException ex) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "QUERY_FAILED", ex.getMessage());
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("error", code, "message", message));
    }
}
