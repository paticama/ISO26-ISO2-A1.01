package com.sescam.radiodiagnostics.common.catalogue;

import java.time.Instant;
import java.util.List;

/**
 * Read/transfer view of a system configuration parameter (UC-ADM-01).
 */
public record ConfigurationParameterDTO(
        String key,
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
        Instant lastModifiedAt
) {
}
