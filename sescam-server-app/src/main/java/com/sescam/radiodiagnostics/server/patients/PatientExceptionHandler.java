package com.sescam.radiodiagnostics.server.patients;

import com.sescam.radiodiagnostics.common.patients.ConcurrentPatientUpdateException;
import com.sescam.radiodiagnostics.common.patients.DuplicatePatientException;
import com.sescam.radiodiagnostics.common.patients.EhrSyncException;
import com.sescam.radiodiagnostics.common.patients.InvalidPatientDataException;
import com.sescam.radiodiagnostics.common.patients.PatientNotFoundException;
import com.sescam.radiodiagnostics.common.patients.UnauthorizedPatientAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Exception handler mapping domain exceptions for UC-AAD-01 to appropriate HTTP status codes.
 */
@RestControllerAdvice(assignableTypes = PatientRestController.class)
public class PatientExceptionHandler {

    @ExceptionHandler(UnauthorizedPatientAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedPatientAccessException ex) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage());
    }

    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(PatientNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(DuplicatePatientException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicatePatientException ex) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_PATIENT", ex.getMessage());
    }

    @ExceptionHandler(InvalidPatientDataException.class)
    public ResponseEntity<Map<String, Object>> handleInvalid(InvalidPatientDataException ex) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_DATA", ex.getMessage());
    }

    @ExceptionHandler(ConcurrentPatientUpdateException.class)
    public ResponseEntity<Map<String, Object>> handleConcurrent(ConcurrentPatientUpdateException ex) {
        return error(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", ex.getMessage());
    }

    @ExceptionHandler(EhrSyncException.class)
    public ResponseEntity<Map<String, Object>> handleEhrSync(EhrSyncException ex) {
        return error(HttpStatus.BAD_GATEWAY, "EHR_SYNC_FAILED", ex.getMessage());
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("error", code, "message", message));
    }
}
