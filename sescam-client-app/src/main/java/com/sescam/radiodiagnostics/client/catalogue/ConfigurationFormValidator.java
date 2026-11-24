package com.sescam.radiodiagnostics.client.catalogue;

import com.sescam.radiodiagnostics.common.catalogue.ConfigurationParameterDTO;
import com.sescam.radiodiagnostics.common.catalogue.ValidationResultDTO;

/**
 * Client-side field validator executed before sending configuration updates to the server.
 */
public class ConfigurationFormValidator {

    public ValidationResultDTO validate(ConfigurationParameterDTO parameter, String proposedValue) {
        if (parameter == null || parameter.key() == null || parameter.key().isBlank()) {
            return ValidationResultDTO.invalid("unknown", "A valid configuration parameter must be selected.");
        }
        if (proposedValue == null || proposedValue.isBlank()) {
            return ValidationResultDTO.invalid(parameter.key(), "Proposed value must not be blank.");
        }

        String trimmed = proposedValue.trim();
        return switch (parameter.valueType()) {
            case INTEGER -> validateInteger(parameter, trimmed);
            case DECIMAL -> validateDecimal(parameter, trimmed);
            case BOOLEAN -> validateBoolean(parameter.key(), trimmed);
            case ENUM -> validateEnum(parameter, trimmed);
            case STRING -> ValidationResultDTO.ok(parameter.key());
        };
    }

    private ValidationResultDTO validateInteger(ConfigurationParameterDTO parameter, String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parameter.minValue() != null && parsed < parameter.minValue()) {
                return ValidationResultDTO.invalid(parameter.key(),
                        "Value must be at least " + parameter.minValue().intValue() + ".");
            }
            if (parameter.maxValue() != null && parsed > parameter.maxValue()) {
                return ValidationResultDTO.invalid(parameter.key(),
                        "Value must be at most " + parameter.maxValue().intValue() + ".");
            }
            return ValidationResultDTO.ok(parameter.key());
        } catch (NumberFormatException ex) {
            return ValidationResultDTO.invalid(parameter.key(), "Value must be a whole integer.");
        }
    }

    private ValidationResultDTO validateDecimal(ConfigurationParameterDTO parameter, String value) {
        try {
            double parsed = Double.parseDouble(value);
            if (Double.isNaN(parsed) || Double.isInfinite(parsed)) {
                return ValidationResultDTO.invalid(parameter.key(), "Value must be a finite decimal number.");
            }
            if (parameter.minValue() != null && parsed < parameter.minValue()) {
                return ValidationResultDTO.invalid(parameter.key(),
                        "Value must be at least " + parameter.minValue() + ".");
            }
            if (parameter.maxValue() != null && parsed > parameter.maxValue()) {
                return ValidationResultDTO.invalid(parameter.key(),
                        "Value must be at most " + parameter.maxValue() + ".");
            }
            return ValidationResultDTO.ok(parameter.key());
        } catch (NumberFormatException ex) {
            return ValidationResultDTO.invalid(parameter.key(), "Value must be a valid decimal number.");
        }
    }

    private ValidationResultDTO validateBoolean(String key, String value) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return ValidationResultDTO.ok(key);
        }
        return ValidationResultDTO.invalid(key, "Value must be 'true' or 'false'.");
    }

    private ValidationResultDTO validateEnum(ConfigurationParameterDTO parameter, String value) {
        if (parameter.allowedValues() != null && parameter.allowedValues().contains(value)) {
            return ValidationResultDTO.ok(parameter.key());
        }
        return ValidationResultDTO.invalid(parameter.key(),
                "Value must be one of: " + parameter.allowedValues());
    }
}
