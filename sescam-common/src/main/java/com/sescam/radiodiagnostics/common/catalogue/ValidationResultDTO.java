package com.sescam.radiodiagnostics.common.catalogue;

/**
 * Result of validating a proposed parameter value before confirmation.
 */
public record ValidationResultDTO(
        boolean valid,
        String parameterKey,
        String errorMessage
) {
    public static ValidationResultDTO ok(String parameterKey) {
        return new ValidationResultDTO(true, parameterKey, null);
    }

    public static ValidationResultDTO invalid(String parameterKey, String errorMessage) {
        return new ValidationResultDTO(false, parameterKey, errorMessage);
    }
}
