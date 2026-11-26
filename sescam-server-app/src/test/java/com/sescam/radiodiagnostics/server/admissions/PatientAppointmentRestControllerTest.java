package com.sescam.radiodiagnostics.server.admissions;

import com.sescam.radiodiagnostics.admissions.internal.InMemoryPendingOrderGateway;
import com.sescam.radiodiagnostics.common.admissions.BookingDTO;
import com.sescam.radiodiagnostics.common.admissions.BookingService;
import com.sescam.radiodiagnostics.common.admissions.CreateBookingRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.PendingExamRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.PendingOrderGateway;
import com.sescam.radiodiagnostics.common.orders.PriorityLevel;
import com.sescam.radiodiagnostics.common.schedules.OpenScheduleRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleService;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end REST test of UC-PAT-01 with the real wiring of the server (C1, C2 stand-in, C3, C4 stand-in, C5).
 * It uses its own order, patient and room, so it does not depend on the data of the UC-AAD-05 test,
 * which shares the same Spring context.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PatientAppointmentRestControllerTest {

    private static final String ORDER_PREFIX = "ORD-PAT01-IT-";

    /** Each test uses its own patient, so the list of one test never contains the other test's appointment. */
    private String patient;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private PendingOrderGateway pendingOrderGateway;
    @Autowired
    private BookingService bookingService;

    @Test
    void patientConsultsConfirmsAsksToMoveAndCancelsTheirAppointment() throws Exception {
        patient = "PAT-901";
        Booked booked = bookAnAbdominalCtFor(patient);
        String bookingId = booked.bookingId();

        // Steps 1-3: the patient sees their appointment with the examination name (from the order, C4).
        asPatient(get("/api/v1/my-appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(bookingId))
                .andExpect(jsonPath("$[0].examTypeName").value("Abdominal CT with contrast"))
                .andExpect(jsonPath("$[0].status").value("BOOKED"));

        // Steps 4-6: details with the preparation instructions of the catalogue and the allowed actions.
        asPatient(get("/api/v1/my-appointments/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("ROUTINE"))
                .andExpect(jsonPath("$.preparation.fastingRequired").value(true))
                .andExpect(jsonPath("$.preparation.preparationGuide").isNotEmpty())
                .andExpect(jsonPath("$.allowedActions").value(hasItem("CONFIRM")))
                .andExpect(jsonPath("$.version").value(1));

        // Steps 7-9: confirmation; a second one is rejected (A7).
        asPatient(post("/api/v1/my-appointments/{id}/confirm", bookingId), """
                {"expectedBookingVersion":1}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.version").value(2));
        asPatient(post("/api/v1/my-appointments/{id}/confirm", bookingId), """
                {"expectedBookingVersion":2}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REQUEST_ALREADY_PROCESSED"));

        // A2: rescheduling request without reason is invalid; with reason it is stored; a second one is A6.
        asPatient(post("/api/v1/my-appointments/{id}/reschedule-requests", bookingId), """
                {"reason":""}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BOOKING_DATA"))
                .andExpect(jsonPath("$.invalidFields[0]").value("reason"));
        String preferred = LocalDate.now().plusDays(10).toString();
        asPatient(post("/api/v1/my-appointments/{id}/reschedule-requests", bookingId), """
                {"reason":"Work trip","preferredDates":["%s"]}""".formatted(preferred))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.preferredDates[0]").value(preferred));
        asPatient(post("/api/v1/my-appointments/{id}/reschedule-requests", bookingId), """
                {"reason":"Still travelling"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PENDING_REQUEST"));
        asPatient(get("/api/v1/my-appointments/{id}", bookingId))
                .andExpect(jsonPath("$.pendingRequest.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.allowedActions").value(not(hasItem("REQUEST_RESCHEDULE"))));

        // A3: a stale version is rejected; with the current version the appointment is cancelled.
        asPatient(post("/api/v1/my-appointments/{id}/cancel", bookingId), """
                {"expectedBookingVersion":1,"reason":"Feeling better"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_BOOKING"));
        asPatient(post("/api/v1/my-appointments/{id}/cancel", bookingId), """
                {"expectedBookingVersion":2,"reason":"Feeling better","priorityWarningAcknowledged":false}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.status").value("CANCELLED"))
                .andExpect(jsonPath("$.allowedActions").isEmpty());

        // Admissions see the order as pending again (same result as a staff cancellation in UC-AAD-05).
        mockMvc.perform(get("/api/v1/bookings/pending-orders")
                        .header("X-Actor-Id", "adm-1")
                        .header("X-Actor-Role", "ADMISSIONS_STAFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].orderId").value(hasItem(booked.orderId())));
    }

    @Test
    void onlyThePatientCanSeeTheirAppointments() throws Exception {
        patient = "PAT-903";
        String bookingId = bookAnAbdominalCtFor(patient).bookingId();

        // A8: another patient is denied.
        mockMvc.perform(get("/api/v1/my-appointments/{id}", bookingId)
                        .header("X-Actor-Id", "PAT-902")
                        .header("X-Actor-Role", "PATIENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_BOOKING_ACCESS"));

        // Staff use the booking area of UC-AAD-05, not the patient area.
        mockMvc.perform(get("/api/v1/my-appointments")
                        .header("X-Actor-Id", "adm-1")
                        .header("X-Actor-Role", "ADMISSIONS_STAFF"))
                .andExpect(status().isForbidden());

        asPatient(get("/api/v1/my-appointments/{id}", "BKG-DOES-NOT-EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));
    }

    // ------------------------------------------------------------------ helpers

    private record Booked(String orderId, String bookingId) {
    }

    /** Staff book a routine abdominal CT for the patient in a room used only by this test (UC-AAD-05). */
    private Booked bookAnAbdominalCtFor(String patientId) {
        String orderId = ORDER_PREFIX + System.nanoTime();
        ((InMemoryPendingOrderGateway) pendingOrderGateway).seed(new PendingExamRequestDTO(orderId, "PHY-PAT01",
                patientId, "CT-ABD-C", "Abdominal CT with contrast", "CT", PriorityLevel.ROUTINE, Instant.now()));
        LocalDate day = LocalDate.now().plusDays(5);
        String room = "RES-PAT01-" + System.nanoTime();
        SlotDTO slot = scheduleService.openSchedule(new OpenScheduleRequestDTO(room, "LOC-MAIN", "CT",
                        day.atTime(9, 0), day.atTime(9, 30), 30, 1, "adm-1", "ADMISSIONS_STAFF"))
                .schedule().slots().get(0);
        BookingDTO booking = bookingService.createBooking(new CreateBookingRequestDTO(orderId, slot.slotId(),
                slot.version(), "adm-1", "ADMISSIONS_STAFF"));
        return new Booked(orderId, booking.bookingId());
    }

    private ResultActions asPatient(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("X-Actor-Id", patient).header("X-Actor-Role", "PATIENT"));
    }

    private ResultActions asPatient(MockHttpServletRequestBuilder request, String jsonBody) throws Exception {
        return asPatient(request.contentType(MediaType.APPLICATION_JSON).content(jsonBody));
    }
}
