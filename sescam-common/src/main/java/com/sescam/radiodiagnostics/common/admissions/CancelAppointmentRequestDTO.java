package com.sescam.radiodiagnostics.common.admissions;

/**
 * The patient cancels an appointment (UC-PAT-01 A3, A4).
 *
 * @param reason                      optional reason given by the patient
 * @param priorityWarningAcknowledged true if the patient saw the clinical priority warning (A4); it is recorded
 *                                    in the audit trail so the cancellation of an urgent examination is traceable
 */
public record CancelAppointmentRequestDTO(
        String bookingId,
        long expectedBookingVersion,
        String reason,
        boolean priorityWarningAcknowledged,
        String actorId,
        String actorRole
) {
}
