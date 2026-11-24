package com.sescam.radiodiagnostics.common.security;

import java.time.Instant;

/**
 * Read-only view of a stored audit event returned by {@link AuditQueryService}.
 */
public record AuditRecordDTO(
        String eventId,
        long sequenceNumber,
        Instant timestamp,
        String actorId,
        String action,
        String resourceKey,
        String previousValue,
        String newValue,
        String details
) {
}
