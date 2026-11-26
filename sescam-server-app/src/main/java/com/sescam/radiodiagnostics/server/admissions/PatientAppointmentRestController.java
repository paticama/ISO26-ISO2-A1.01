package com.sescam.radiodiagnostics.server.admissions;

import com.sescam.radiodiagnostics.common.admissions.AppointmentDetailsDTO;
import com.sescam.radiodiagnostics.common.admissions.AppointmentSummaryDTO;
import com.sescam.radiodiagnostics.common.admissions.CancelAppointmentRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.ConfirmAppointmentRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.PatientAppointmentService;
import com.sescam.radiodiagnostics.common.admissions.PatientRescheduleRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.RescheduleRequestStatusDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST API for UC-PAT-01: Manage my appointments (C5 v1.1.0).
 *
 * <p>The caller is the authenticated patient: X-Actor-Id is their patientId and X-Actor-Role must be PATIENT.
 * Errors are mapped by {@link BookingExceptionHandler}, shared with UC-AAD-05.</p>
 */
@RestController
@RequestMapping("/api/v1/my-appointments")
public class PatientAppointmentRestController {

    private final PatientAppointmentService appointmentService;

    public PatientAppointmentRestController(PatientAppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /** Steps 1-3: upcoming appointments in chronological order; an empty list is A1. */
    @GetMapping
    public ResponseEntity<List<AppointmentSummaryDTO>> listMyAppointments(
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.ok(appointmentService.listMyAppointments(actorId, actorRole));
    }

    /** Steps 4-6: details, preparation instructions and allowed actions. */
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDetailsDTO> getAppointmentDetails(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.ok(appointmentService.getAppointmentDetails(id, actorId, actorRole));
    }

    /** Steps 7-9: the patient confirms attendance. */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<AppointmentDetailsDTO> confirmAppointment(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestBody ConfirmPayload payload) {
        return ResponseEntity.ok(appointmentService.confirmAppointment(
                new ConfirmAppointmentRequestDTO(id, payload.expectedBookingVersion(), actorId, actorRole)));
    }

    /** A2: the patient asks Admissions to move the appointment (201: a new request is created). */
    @PostMapping("/{id}/reschedule-requests")
    public ResponseEntity<RescheduleRequestStatusDTO> requestRescheduling(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestBody ReschedulePayload payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.requestRescheduling(
                new PatientRescheduleRequestDTO(id, payload.reason(), payload.preferredDates(), actorId, actorRole)));
    }

    /** A3, A4: the patient cancels the appointment. */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<AppointmentDetailsDTO> cancelAppointment(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestBody CancelPayload payload) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(new CancelAppointmentRequestDTO(
                id, payload.expectedBookingVersion(), payload.reason(), payload.priorityWarningAcknowledged(),
                actorId, actorRole)));
    }

    public record ConfirmPayload(long expectedBookingVersion) {}

    public record ReschedulePayload(String reason, List<LocalDate> preferredDates) {}

    public record CancelPayload(long expectedBookingVersion, String reason, boolean priorityWarningAcknowledged) {}
}
