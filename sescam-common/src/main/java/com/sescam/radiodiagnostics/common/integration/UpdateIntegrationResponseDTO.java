package com.sescam.radiodiagnostics.common.integration;

import java.time.Instant;

/**
 * Confirmation returned after the settings were saved (main flow, step 7).
 */
public record UpdateIntegrationResponseDTO(
        String integrationId,
        IntegrationStatus status,
        String message,
        long newVersion,
        String updatedBy,
        Instant updatedAt
) {
}
