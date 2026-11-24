package com.sescam.radiodiagnostics.common.diagnostics;

import java.util.List;

/**
 * Mandatory execution data missing or inconsistent. HTTP 400.
 */
public class InvalidExecutionDataException extends RuntimeException {

    private final List<String> invalidFields;

    public InvalidExecutionDataException(String message, List<String> invalidFields) {
        super(message);
        this.invalidFields = List.copyOf(invalidFields);
    }

    /** Names of the {@link ExecutionLogDTO} components that failed, e.g. {@code doseDlp} or {@code incidents[0].severity}. */
    public List<String> getInvalidFields() {
        return invalidFields;
    }
}
