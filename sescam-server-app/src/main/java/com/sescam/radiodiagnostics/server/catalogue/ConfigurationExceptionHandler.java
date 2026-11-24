package com.sescam.radiodiagnostics.server.catalogue;

import com.sescam.radiodiagnostics.common.catalogue.ConcurrentConfigurationChangeException;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationNotFoundException;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationSaveException;
import com.sescam.radiodiagnostics.common.catalogue.InvalidConfigurationValueException;
import com.sescam.radiodiagnostics.common.catalogue.UnauthorizedConfigurationAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ConfigurationExceptionHandler {

    @ExceptionHandler(UnauthorizedConfigurationAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedConfigurationAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "FORBIDDEN", "message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidConfigurationValueException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidValue(InvalidConfigurationValueException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_VALUE", "parameterKey", ex.getParameterKey(), "message", ex.getMessage()));
    }

    @ExceptionHandler(ConfigurationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ConfigurationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "parameterKey", ex.getParameterKey(), "message", ex.getMessage()));
    }

    @ExceptionHandler(ConcurrentConfigurationChangeException.class)
    public ResponseEntity<Map<String, Object>> handleConcurrent(ConcurrentConfigurationChangeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "CONCURRENT_MODIFICATION",
                        "parameterKey", ex.getParameterKey(),
                        "expectedVersion", ex.getExpectedVersion(),
                        "actualVersion", ex.getActualVersion(),
                        "currentStoredValue", ex.getCurrentStoredValue(),
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(ConfigurationSaveException.class)
    public ResponseEntity<Map<String, Object>> handleSaveFailure(ConfigurationSaveException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "SAVE_FAILED",
                        "parameterKey", ex.getParameterKey(),
                        "previousValue", ex.getPreviousValue(),
                        "message", ex.getMessage()
                ));
    }
}
