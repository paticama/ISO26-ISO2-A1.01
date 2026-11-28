package com.sescam.radiodiagnostics.common.admissions;

import java.time.Instant;

/** Immutable audit trail entry of a booking status or slot transition (A2, A3). */
public record BookingHistoryEntryDTO(
        String entryId,
        BookingStatus previousStatus,
        BookingStatus newStatus,
        String previousSlotId,
        String newSlotId,
        String reason,
        String changedBy,
        Instant changedAt
) {
}
