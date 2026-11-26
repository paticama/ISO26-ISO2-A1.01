package com.sescam.radiodiagnostics.common.admissions;

/**
 * The patient confirms they will attend (UC-PAT-01 step 7).
 *
 * @param expectedBookingVersion version shown to the patient; a different stored version means the booking
 *                               changed meanwhile (CONCURRENT_BOOKING)
 */
public record ConfirmAppointmentRequestDTO(
        String bookingId,
        long expectedBookingVersion,
        String actorId,
        String actorRole
) {
}
