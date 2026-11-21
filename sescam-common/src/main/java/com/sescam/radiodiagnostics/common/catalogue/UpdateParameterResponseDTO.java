package com.sescam.radiodiagnostics.common.catalogue;

import java.time.Instant;

/**
 * Response returned after a confirmed configuration parameter update.
 */
public record UpdateParameterResponseDTO(
        String parameterKey,
        String previousValue,
        String currentValue,
        long newVersion,
        ActivationStatus activationStatus,
        String message,
        String updatedBy,
        Instant updatedAt
) {
}
