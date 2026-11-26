package com.sescam.radiodiagnostics.common.admissions;

import java.time.Instant;
import java.time.LocalDateTime;

/** Domain event emitted when a booking is created, rescheduled, or cancelled (ADR-014). */
public record BookingChangedEvent(
        String bookingId,
        String orderId,
        String patientId,
        String slotId,
        BookingStatus status,
        LocalDateTime scheduledStart,
        Instant occurredAt
) {
}
