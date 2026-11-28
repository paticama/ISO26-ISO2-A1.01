package com.sescam.radiodiagnostics.server.diagnostics;

import com.sescam.radiodiagnostics.common.diagnostics.InvalidReportDataException;
import com.sescam.radiodiagnostics.common.diagnostics.PacsConnectionException;
import com.sescam.radiodiagnostics.common.diagnostics.ReportNotFoundException;
import com.sescam.radiodiagnostics.common.diagnostics.ReportStateConflictException;
import com.sescam.radiodiagnostics.common.diagnostics.UnauthorizedReportingAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps domain exceptions of UC-PHY-04 to HTTP status codes.
 */
@RestControllerAdvice(assignableTypes = DiagnosticReportRestController.class)
public class DiagnosticReportExceptionHandler {

    @ExceptionHandler(UnauthorizedReportingAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedReportingAccessException ex) {
        return body(HttpStatus.FORBIDDEN, Map.of("error", "FORBIDDEN", "message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidReportDataException.class)
    public ResponseEntity<Map<String, Object>> handleInvalid(InvalidReportDataException ex) {
        return body(HttpStatus.BAD_REQUEST, Map.of("error", "INVALID_DATA", "message", ex.getMessage(),
                "invalidFields", ex.getInvalidFields()));
    }

    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ReportNotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, Map.of("error", "REPORT_NOT_FOUND", "message", ex.getMessage()));
    }

    @ExceptionHandler(ReportStateConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ReportStateConflictException ex) {
        return body(HttpStatus.CONFLICT, Map.of("error", "REPORT_STATE_CONFLICT", "message", ex.getMessage()));
    }

    @ExceptionHandler(PacsConnectionException.class)
    public ResponseEntity<Map<String, Object>> handlePacsError(PacsConnectionException ex) {
        return body(HttpStatus.BAD_GATEWAY, Map.of("error", "PACS_CONNECTION_ERROR", "message", ex.getMessage()));
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, Map<String, Object> body) {
        return ResponseEntity.status(status).body(body);
    }
}
