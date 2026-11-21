package com.sescam.radiodiagnostics.catalogue.domain;

import com.sescam.radiodiagnostics.common.catalogue.ActivationMode;
import com.sescam.radiodiagnostics.common.catalogue.ActivationStatus;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationParameterDTO;
import com.sescam.radiodiagnostics.common.catalogue.ParameterValueType;
import com.sescam.radiodiagnostics.common.catalogue.ValidationResultDTO;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Core domain entity representing a configurable system-wide parameter in C2 (sescam-catalogue).
 * Pure domain object free of framework/persistence annotations (ADR-003).
 */
public class ConfigurationParameter {

    private final String key;
    private final String name;
    private final String category;
    private final String description;
    private String currentValue;
    private final ParameterValueType valueType;
    private final Double minValue;
    private final Double maxValue;
    private final List<String> allowedValues;
    private final ActivationMode activationMode;
    private long version;
    private String lastModifiedBy;
    private Instant lastModifiedAt;

    public ConfigurationParameter(String key,
                                  String name,
                                  String category,
                                  String description,
                                  String currentValue,
                                  ParameterValueType valueType,
                                  Double minValue,
                                  Double maxValue,
                                  List<String> allowedValues,
                                  ActivationMode activationMode,
                                  long version,
                                  String lastModifiedBy,
                                  Instant lastModifiedAt) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.currentValue = Objects.requireNonNull(currentValue, "currentValue must not be null");
        this.valueType = Objects.requireNonNull(valueType, "valueType must not be null");
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.allowedValues = allowedValues == null ? List.of() : List.copyOf(allowedValues);
        this.activationMode = Objects.requireNonNull(activationMode, "activationMode must not be null");
        this.version = version;
        this.lastModifiedBy = lastModifiedBy;
        this.lastModifiedAt = lastModifiedAt;
    }

    /**
     * Validates a proposed value against this parameter's type, numeric range, and allowed values.
     */
    public ValidationResultDTO validateProposedValue(String proposedValue) {
        if (proposedValue == null || proposedValue.isBlank()) {
            return ValidationResultDTO.invalid(key, "Value must not be null or blank.");
        }
        String trimmed = proposedValue.trim();

        return switch (valueType) {
            case INTEGER -> validateInteger(trimmed);
            case DECIMAL -> validateDecimal(trimmed);
            case BOOLEAN -> validateBoolean(trimmed);
            case ENUM -> validateEnum(trimmed);
            case STRING -> ValidationResultDTO.ok(key);
        };
    }

    private ValidationResultDTO validateInteger(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (minValue != null && parsed < minValue) {
                return ValidationResultDTO.invalid(key, "Value " + parsed + " is below minimum allowed (" + minValue.intValue() + ").");
            }
            if (maxValue != null && parsed > maxValue) {
                return ValidationResultDTO.invalid(key, "Value " + parsed + " exceeds maximum allowed (" + maxValue.intValue() + ").");
            }
            return ValidationResultDTO.ok(key);
        } catch (NumberFormatException ex) {
            return ValidationResultDTO.invalid(key, "Value '" + value + "' is not a valid integer.");
        }
    }

    private ValidationResultDTO validateDecimal(String value) {
        try {
            double parsed = Double.parseDouble(value);
            if (Double.isNaN(parsed) || Double.isInfinite(parsed)) {
                return ValidationResultDTO.invalid(key, "Value '" + value + "' is not a finite decimal number.");
            }
            if (minValue != null && parsed < minValue) {
                return ValidationResultDTO.invalid(key, "Value " + parsed + " is below minimum allowed (" + minValue + ").");
            }
            if (maxValue != null && parsed > maxValue) {
                return ValidationResultDTO.invalid(key, "Value " + parsed + " exceeds maximum allowed (" + maxValue + ").");
            }
            return ValidationResultDTO.ok(key);
        } catch (NumberFormatException ex) {
            return ValidationResultDTO.invalid(key, "Value '" + value + "' is not a valid decimal number.");
        }
    }

    private ValidationResultDTO validateBoolean(String value) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return ValidationResultDTO.ok(key);
        }
        return ValidationResultDTO.invalid(key, "Value '" + value + "' must be 'true' or 'false'.");
    }

    private ValidationResultDTO validateEnum(String value) {
        if (allowedValues.contains(value)) {
            return ValidationResultDTO.ok(key);
        }
        return ValidationResultDTO.invalid(key, "Value '" + value + "' is not in allowed values: " + allowedValues);
    }

    /**
     * Produces a new copy with updated value, incremented optimistic version, and audit metadata.
     */
    public ConfigurationParameter withUpdatedValue(String newValue, String administratorId, Instant timestamp) {
        return new ConfigurationParameter(
                key,
                name,
                category,
                description,
                newValue.trim(),
                valueType,
                minValue,
                maxValue,
                allowedValues,
                activationMode,
                version + 1,
                administratorId,
                timestamp
        );
    }

    public ActivationStatus resolveActivationStatus() {
        return switch (activationMode) {
            case IMMEDIATE -> ActivationStatus.ACTIVE;
            case REQUIRES_RELOAD -> ActivationStatus.PENDING_RELOAD;
            case REQUIRES_RESTART -> ActivationStatus.PENDING_RESTART;
        };
    }

    public ConfigurationParameterDTO toDTO() {
        return new ConfigurationParameterDTO(
                key,
                name,
                category,
                description,
                currentValue,
                valueType,
                minValue,
                maxValue,
                allowedValues,
                activationMode,
                version,
                lastModifiedBy,
                lastModifiedAt
        );
    }

    public String getKey() {
        return key;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public long getVersion() {
        return version;
    }

    public ActivationMode getActivationMode() {
        return activationMode;
    }
}
