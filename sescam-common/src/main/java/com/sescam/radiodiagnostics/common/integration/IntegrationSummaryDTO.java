package com.sescam.radiodiagnostics.common.integration;

import java.time.Instant;

/**
 * What the administrator sees of an integration: settings and status. It never carries the credential,
 * only whether one is stored ({@code credentialConfigured}).
 */
public record IntegrationSummaryDTO(
        String id,
        IntegrationType type,
        String displayName,
        String endpointUrl,
        String username,
        boolean credentialConfigured,
        IntegrationStatus status,
        String statusMessage,
        long version,
        Instant lastCheckedAt,
        String lastModifiedBy,
        Instant lastModifiedAt
) {
}
