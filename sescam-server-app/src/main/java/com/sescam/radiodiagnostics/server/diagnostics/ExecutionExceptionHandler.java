package com.sescam.radiodiagnostics.server.diagnostics;

import com.sescam.radiodiagnostics.common.diagnostics.ExecutionAlreadyRecordedException;
import com.sescam.radiodiagnostics.common.diagnostics.InvalidExecutionDataException;
import com.sescam.radiodiagnostics.common.diagnostics.ProcedureNotFoundException;
import com.sescam.radiodiagnostics.common.diagnostics.UnauthorizedExecutionAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps the domain exceptions of UC-PHY-05 to HTTP status codes.
 */
@RestControllerAdvice(assignableTypes = ExecutionRestController.class)
public class ExecutionExceptionHandler {

    @ExceptionHandler(UnauthorizedExecutionAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedExecutionAccessException ex) {
        return body(HttpStatus.FORBIDDEN, Map.of("error", "FORBIDDEN", "message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidExecutionDataException.class)
    public ResponseEntity<Map<String, Object>> handleInvalid(InvalidExecutionDataException ex) {
        return body(HttpStatus.BAD_REQUEST, Map.of("error", "INVALID_DATA", "message", ex.getMessage(),
                "invalidFields", ex.getInvalidFields()));
    }

    @ExceptionHandler(ProcedureNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ProcedureNotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, Map.of("error", "PROCEDURE_NOT_FOUND", "message", ex.getMessage()));
    }

    @ExceptionHandler(ExecutionAlreadyRecordedException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyRecorded(ExecutionAlreadyRecordedException ex) {
        return body(HttpStatus.CONFLICT, Map.of("error", "ALREADY_RECORDED", "message", ex.getMessage()));
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, Map<String, Object> body) {
        return ResponseEntity.status(status).body(body);
    }
}
