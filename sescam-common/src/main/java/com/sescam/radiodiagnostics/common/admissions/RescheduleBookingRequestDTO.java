package com.sescam.radiodiagnostics.common.admissions;

/** Request to reschedule an existing booking to a new slot (A2). */
public record RescheduleBookingRequestDTO(
        String bookingId,
        String newSlotId,
        long expectedSlotVersion,
        long expectedBookingVersion,
        String reason,
        String actorId,
        String actorRole
) {
}
