package com.sescam.radiodiagnostics.server.orders;

import com.sescam.radiodiagnostics.common.catalogue.ExamTypeNotFoundException;
import com.sescam.radiodiagnostics.common.orders.InvalidOrderDataException;
import com.sescam.radiodiagnostics.common.orders.SafetyAlertException;
import com.sescam.radiodiagnostics.common.orders.UnauthorizedOrderAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps the domain exceptions of UC-PHY-01 to HTTP status codes.
 */
@RestControllerAdvice(assignableTypes = OrderRestController.class)
public class OrderExceptionHandler {

    @ExceptionHandler(UnauthorizedOrderAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedOrderAccessException ex) {
        return body(HttpStatus.FORBIDDEN, Map.of("error", "FORBIDDEN", "message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidOrderDataException.class)
    public ResponseEntity<Map<String, Object>> handleInvalid(InvalidOrderDataException ex) {
        return body(HttpStatus.BAD_REQUEST, Map.of("error", "INVALID_DATA", "message", ex.getMessage(),
                "invalidFields", ex.getInvalidFields()));
    }

    @ExceptionHandler(ExamTypeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleExamNotFound(ExamTypeNotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, Map.of("error", "EXAM_TYPE_NOT_FOUND", "message", ex.getMessage()));
    }

    /** A6b: the client shows the constraints and resubmits with a justification, or cancels. */
    @ExceptionHandler(SafetyAlertException.class)
    public ResponseEntity<Map<String, Object>> handleSafetyAlert(SafetyAlertException ex) {
        return body(HttpStatus.UNPROCESSABLE_ENTITY, Map.of("error", "SAFETY_ALERT", "message", ex.getMessage(),
                "constraints", ex.getConstraints()));
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, Map<String, Object> body) {
        return ResponseEntity.status(status).body(body);
    }
}
