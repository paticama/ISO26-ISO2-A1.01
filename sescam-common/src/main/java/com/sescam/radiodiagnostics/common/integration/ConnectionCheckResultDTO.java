package com.sescam.radiodiagnostics.common.integration;

import java.time.Instant;

/**
 * Result of a connection check. The message explains the outcome and never contains secrets.
 */
public record ConnectionCheckResultDTO(
        String integrationId,
        IntegrationStatus status,
        String message,
        Instant checkedAt
) {
}
