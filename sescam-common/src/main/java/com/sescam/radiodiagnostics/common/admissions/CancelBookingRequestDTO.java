package com.sescam.radiodiagnostics.common.admissions;

/** Request to cancel an existing booking with an explicit reason (A3). */
public record CancelBookingRequestDTO(
        String bookingId,
        long expectedBookingVersion,
        String reason,
        String actorId,
        String actorRole
) {
}
