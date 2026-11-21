package com.sescam.radiodiagnostics.common.security;

import java.time.Instant;

/**
 * Immutable audit record for security and configuration traceability (NFR-11, UC-ADM-03).
 */
public record AuditEventDTO(
        String action,
        String actorId,
        String resourceKey,
        String previousValue,
        String newValue,
        String details,
        Instant timestamp
) {
}
