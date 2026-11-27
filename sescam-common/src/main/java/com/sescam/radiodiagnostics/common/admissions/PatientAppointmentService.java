package com.sescam.radiodiagnostics.common.admissions;

import java.util.List;

/**
 * Patient-facing operations on their own appointments (UC-PAT-01 Manage my appointments, C5 v1.1.0).
 *
 * <p>Every operation is done by an authenticated patient: {@code actorId} is their patientId and
 * {@code actorRole} must be PATIENT. A patient can only see and change their own appointments (A8).
 * Staff operations on the same bookings are in {@link BookingService} (UC-AAD-05).</p>
 *
 * <p>Errors are reported with the {@link BookingException} hierarchy shared with UC-AAD-05:
 * UNAUTHORIZED_BOOKING_ACCESS (403), BOOKING_NOT_FOUND (404), INVALID_BOOKING_DATA (400),
 * REQUEST_ALREADY_PROCESSED, CONCURRENT_BOOKING, BOOKING_CONFLICT and DUPLICATE_PENDING_REQUEST (409).</p>
 */
public interface PatientAppointmentService {

    /** Steps 1-3 (+A1): the patient's appointments from today on, in chronological order (may be empty). */
    List<AppointmentSummaryDTO> listMyAppointments(String actorId, String actorRole);

    /** Steps 4-6 (+A7, A8): details, preparation instructions and the actions the policy allows now. */
    AppointmentDetailsDTO getAppointmentDetails(String bookingId, String actorId, String actorRole);

    /** Steps 7-9 (+A5, A7): the patient confirms attendance; the booking becomes CONFIRMED. */
    AppointmentDetailsDTO confirmAppointment(ConfirmAppointmentRequestDTO request);

    /** A2 (+A6): stores a rescheduling request as PENDING_REVIEW for Admissions; the booking is not changed. */
    RescheduleRequestStatusDTO requestRescheduling(PatientRescheduleRequestDTO request);

    /** A3, A4 (+A5, A7): the patient cancels; the slot is released and Admissions and the patient are informed. */
    AppointmentDetailsDTO cancelAppointment(CancelAppointmentRequestDTO request);
}
