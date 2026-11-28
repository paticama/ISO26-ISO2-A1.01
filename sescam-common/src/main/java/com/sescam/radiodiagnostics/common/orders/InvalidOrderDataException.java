package com.sescam.radiodiagnostics.common.orders;

import java.util.List;

/**
 * Mandatory data missing or invalid (A4a, enforced again on the server). HTTP 400.
 */
public class InvalidOrderDataException extends RuntimeException {

    private final List<String> invalidFields;

    public InvalidOrderDataException(String message, List<String> invalidFields) {
        super(message);
        this.invalidFields = List.copyOf(invalidFields);
    }

    /** Names of the {@link CreateOrderDTO} components that failed validation. */
    public List<String> getInvalidFields() {
        return invalidFields;
    }
}
