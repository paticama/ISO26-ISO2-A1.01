package com.sescam.radiodiagnostics.common.integration;

/**
 * Request to save new settings of an integration, with optimistic concurrency control.
 */
public record UpdateIntegrationRequestDTO(
        String integrationId,
        IntegrationSettingsDTO settings,
        long expectedVersion,
        String administratorId,
        String administratorRole
) {
}
