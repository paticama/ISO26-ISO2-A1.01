package com.sescam.radiodiagnostics.common.diagnostics;

import java.util.List;

/**
 * Report payload missing mandatory clinical findings, conclusion, or digital signature. HTTP 400.
 */
public class InvalidReportDataException extends RuntimeException {

    private final List<String> invalidFields;

    public InvalidReportDataException(String message, List<String> invalidFields) {
        super(message);
        this.invalidFields = invalidFields == null ? List.of() : List.copyOf(invalidFields);
    }

    public List<String> getInvalidFields() {
        return invalidFields;
    }
}
