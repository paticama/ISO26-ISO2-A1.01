package com.sescam.radiodiagnostics.admissions.service;

import com.sescam.radiodiagnostics.admissions.domain.Booking;
import com.sescam.radiodiagnostics.admissions.internal.InMemoryPendingOrderGateway;
import com.sescam.radiodiagnostics.admissions.repository.InMemoryBookingRepository;
import com.sescam.radiodiagnostics.admissions.repository.InMemoryRescheduleRequestRepository;
import com.sescam.radiodiagnostics.admissions.repository.InMemoryWorklistRepository;
import com.sescam.radiodiagnostics.common.admissions.AppointmentAction;
import com.sescam.radiodiagnostics.common.admissions.AppointmentDetailsDTO;
import com.sescam.radiodiagnostics.common.admissions.AppointmentSummaryDTO;
import com.sescam.radiodiagnostics.common.admissions.BookingChangedEvent;
import com.sescam.radiodiagnostics.common.admissions.BookingConflictException;
import com.sescam.radiodiagnostics.common.admissions.BookingNotFoundException;
import com.sescam.radiodiagnostics.common.admissions.BookingNotificationGateway;
import com.sescam.radiodiagnostics.common.admissions.BookingPolicyGateway;
import com.sescam.radiodiagnostics.common.admissions.BookingStatus;
import com.sescam.radiodiagnostics.common.admissions.CancelAppointmentRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.ConcurrentBookingException;
import com.sescam.radiodiagnostics.common.admissions.ConfirmAppointmentRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.DuplicatePendingRequestException;
import com.sescam.radiodiagnostics.common.admissions.InvalidBookingDataException;
import com.sescam.radiodiagnostics.common.admissions.NotificationStatus;
import com.sescam.radiodiagnostics.common.admissions.PatientRescheduleRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.PendingExamRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.RequestAlreadyProcessedException;
import com.sescam.radiodiagnostics.common.admissions.RescheduleRequestStatus;
import com.sescam.radiodiagnostics.common.admissions.RescheduleRequestStatusDTO;
import com.sescam.radiodiagnostics.common.admissions.UnauthorizedBookingAccessException;
import com.sescam.radiodiagnostics.common.catalogue.ExamTypeDTO;
import com.sescam.radiodiagnostics.common.catalogue.ExamTypeNotFoundException;
import com.sescam.radiodiagnostics.common.catalogue.ExamTypeService;
import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.orders.PriorityLevel;
import com.sescam.radiodiagnostics.common.schedules.ScheduleService;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * UC-PAT-01 Manage my appointments: one test (at least) per acceptance criterion and per alternative flow.
 * Repositories, order gateway, audit, events, notification and catalogue are real in-memory fakes;
 * only the schedule module (C3) is a mock, to check that the slot is released.
 */
@ExtendWith(MockitoExtension.class)
class PatientAppointmentServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.of(2026, 11, 26, 10, 0);
    private static final String PATIENT = "PAT-001";
    private static final String OTHER_PATIENT = "PAT-002";
    private static final String ROLE = "PATIENT";
    private static final String ROUTINE_ORDER = "ORD-2001";
    private static final String URGENT_ORDER = "ORD-1001";   // seeded by InMemoryPendingOrderGateway, STAT_URGENT

    @Mock
    private ScheduleService scheduleService;

    private final List<AuditEventDTO> audited = new ArrayList<>();
    private final List<BookingChangedEvent> published = new ArrayList<>();
    private final RecordingNotifications notifications = new RecordingNotifications();
    private final TestPolicy policy = new TestPolicy();

    private InMemoryBookingRepository bookings;
    private InMemoryRescheduleRequestRepository requests;
    private InMemoryWorklistRepository worklist;
    private InMemoryPendingOrderGateway orders;
    private PatientAppointmentServiceImpl service;

    @BeforeEach
    void setUp() {
        bookings = new InMemoryBookingRepository();
        requests = new InMemoryRescheduleRequestRepository();
        worklist = new InMemoryWorklistRepository();
        orders = new InMemoryPendingOrderGateway();
        orders.seed(new PendingExamRequestDTO(ROUTINE_ORDER, "PHY-2026-2001", PATIENT, "CT-ABD-C",
                "Abdominal CT with contrast", "CT", PriorityLevel.ROUTINE, Instant.parse("2026-11-20T09:00:00Z")));
        AccessControlService accessControl = (actorId, role) -> false; // only the default methods are used
        service = new PatientAppointmentServiceImpl(bookings, requests, worklist, scheduleService, orders,
                new CatalogueWithAbdominalCt(), policy, notifications, accessControl, audited::add, published::add,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // ---------------------------------------------------------------- helpers

    private Booking bookingOf(String bookingId, String orderId, String patientId, LocalDateTime start) {
        SlotDTO slot = new SlotDTO("SLOT-" + bookingId, "SCH-1", "CT-ROOM-1", "LOC-MAIN", "CT",
                start, start.plusMinutes(30), 1, 0, 1L, true);
        Booking booking = new Booking(bookingId, orderId, patientId, "CT", slot, "adm-1", NOW.minusSeconds(86_400));
        bookings.save(booking);
        return booking;
    }

    private Booking routineBookingInThreeDays() {
        return bookingOf("BKG-1", ROUTINE_ORDER, PATIENT, NOW_LOCAL.plusDays(3));
    }

    private List<String> auditedActions() {
        return audited.stream().map(AuditEventDTO::action).toList();
    }

    private AuditEventDTO lastAudit(String action) {
        return audited.stream().filter(e -> e.action().equals(action)).reduce((a, b) -> b).orElseThrow();
    }

    // ---------------------------------------------------------------- steps 1-3: list (+A1)

    @Test
    void listsOnlyThePatientsAppointmentsFromTodayInChronologicalOrder() {
        bookingOf("BKG-LATER", ROUTINE_ORDER, PATIENT, NOW_LOCAL.plusDays(5));
        bookingOf("BKG-SOON", ROUTINE_ORDER, PATIENT, NOW_LOCAL.plusDays(1));
        bookingOf("BKG-PAST", ROUTINE_ORDER, PATIENT, NOW_LOCAL.minusDays(1));
        bookingOf("BKG-OTHER", "ORD-1002", OTHER_PATIENT, NOW_LOCAL.plusDays(2));

        List<AppointmentSummaryDTO> appointments = service.listMyAppointments(PATIENT, ROLE);

        assertEquals(List.of("BKG-SOON", "BKG-LATER"), appointments.stream().map(AppointmentSummaryDTO::bookingId).toList());
        AppointmentSummaryDTO first = appointments.get(0);
        assertEquals("Abdominal CT with contrast", first.examTypeName());
        assertEquals("CT", first.modality());
        assertEquals("LOC-MAIN", first.locationId());
        assertEquals("CT-ROOM-1", first.resourceId());
        assertEquals(BookingStatus.BOOKED, first.status());
        assertEquals(List.of("APPOINTMENTS_VIEWED"), auditedActions());
    }

    @Test
    void a1_returnsAnEmptyListWhenThePatientHasNoUpcomingAppointments() {
        assertTrue(service.listMyAppointments(PATIENT, ROLE).isEmpty());
        assertEquals(List.of("APPOINTMENTS_VIEWED"), auditedActions());
    }

    @Test
    void staffCannotUseThePatientAppointmentArea() {
        assertThrows(UnauthorizedBookingAccessException.class,
                () -> service.listMyAppointments("adm-1", "ADMISSIONS_STAFF"));
        assertEquals(List.of("APPOINTMENT_ACCESS_DENIED"), auditedActions());
    }

    // ---------------------------------------------------------------- steps 4-6: details (+A7, A8)

    @Test
    void detailsIncludePreparationInstructionsPriorityAndAllowedActions() {
        Booking booking = routineBookingInThreeDays();

        AppointmentDetailsDTO details = service.getAppointmentDetails(booking.getBookingId(), PATIENT, ROLE);

        assertEquals("BKG-1", details.summary().bookingId());
        assertEquals("CT-ABD-C", details.examTypeCode());
        assertEquals(PriorityLevel.ROUTINE, details.priority());
        assertEquals("Fast for 6 hours.", details.preparation().preparationGuide());
        assertTrue(details.preparation().fastingRequired());
        assertTrue(details.preparation().contrastRequired());
        assertTrue(details.preparation().informedConsentRequired());
        assertEquals(Set.of(AppointmentAction.CONFIRM, AppointmentAction.REQUEST_RESCHEDULE, AppointmentAction.CANCEL),
                details.allowedActions());
        assertNull(details.pendingRequest());
        assertEquals(TestPolicy.CONTACT, details.serviceContact());
        assertEquals(1L, details.version());
        assertEquals(List.of("APPOINTMENT_DETAILS_VIEWED"), auditedActions());
    }

    @Test
    void detailsStillLoadWhenTheCatalogueDoesNotKnowTheExamination() {
        // ORD-1001 asks for CT-CHEST, which the catalogue stand-in does not have.
        bookingOf("BKG-URG", URGENT_ORDER, PATIENT, NOW_LOCAL.plusDays(2));

        AppointmentDetailsDTO details = service.getAppointmentDetails("BKG-URG", PATIENT, ROLE);

        assertNull(details.preparation());
        assertEquals(PriorityLevel.STAT_URGENT, details.priority());
    }

    @Test
    void a7_aCancelledAppointmentShowsItsStatusWithoutActions() {
        Booking booking = routineBookingInThreeDays();
        booking.cancel("Staff cancellation", "adm-1", NOW);

        AppointmentDetailsDTO details = service.getAppointmentDetails("BKG-1", PATIENT, ROLE);

        assertEquals(BookingStatus.CANCELLED, details.summary().status());
        assertTrue(details.allowedActions().isEmpty());
    }

    @Test
    void a8_anotherPatientsAppointmentIsDeniedAndAudited() {
        routineBookingInThreeDays();

        assertThrows(UnauthorizedBookingAccessException.class,
                () -> service.getAppointmentDetails("BKG-1", OTHER_PATIENT, ROLE));
        AuditEventDTO denied = lastAudit("APPOINTMENT_ACCESS_DENIED");
        assertEquals(OTHER_PATIENT, denied.actorId());
        assertEquals("BKG-1", denied.resourceKey());
    }

    @Test
    void anUnknownAppointmentIsNotFound() {
        assertThrows(BookingNotFoundException.class, () -> service.getAppointmentDetails("BKG-404", PATIENT, ROLE));
    }

    @Test
    void aMissingAppointmentIdIsInvalid() {
        InvalidBookingDataException ex = assertThrows(InvalidBookingDataException.class,
                () -> service.getAppointmentDetails(" ", PATIENT, ROLE));
        assertEquals(List.of("bookingId"), ex.getInvalidFields());
    }

    // ---------------------------------------------------------------- steps 7-9: confirm (+A5, A7)

    @Test
    void confirmingChangesTheStatusToConfirmedAuditsAndPublishes() {
        routineBookingInThreeDays();

        AppointmentDetailsDTO details = service.confirmAppointment(
                new ConfirmAppointmentRequestDTO("BKG-1", 1L, PATIENT, ROLE));

        assertEquals(BookingStatus.CONFIRMED, details.summary().status());
        assertEquals(2L, details.version());
        assertEquals(Set.of(AppointmentAction.REQUEST_RESCHEDULE, AppointmentAction.CANCEL), details.allowedActions());
        assertEquals(BookingStatus.CONFIRMED, bookings.findById("BKG-1").orElseThrow().getStatus());
        AuditEventDTO audit = lastAudit("APPOINTMENT_CONFIRMED");
        assertEquals(PATIENT, audit.actorId());
        assertEquals("BOOKED", audit.previousValue());
        assertEquals("CONFIRMED", audit.newValue());
        assertEquals(NOW, audit.timestamp());
        assertEquals(1, published.size());
        assertEquals(BookingStatus.CONFIRMED, published.get(0).status());
    }

    @Test
    void a5_confirmingAfterTheDeadlineIsRejectedWithTheServiceContactAndChangesNothing() {
        policy.confirmHours = 48;
        bookingOf("BKG-1", ROUTINE_ORDER, PATIENT, NOW_LOCAL.plusDays(1));

        BookingConflictException ex = assertThrows(BookingConflictException.class,
                () -> service.confirmAppointment(new ConfirmAppointmentRequestDTO("BKG-1", 1L, PATIENT, ROLE)));

        assertTrue(ex.getMessage().contains(TestPolicy.CONTACT));
        Booking stored = bookings.findById("BKG-1").orElseThrow();
        assertEquals(BookingStatus.BOOKED, stored.getStatus());
        assertEquals(1L, stored.getVersion());
        assertEquals(List.of("APPOINTMENT_ACTION_REJECTED"), auditedActions());
        assertTrue(published.isEmpty());
    }

    @Test
    void confirmingWithAStaleVersionIsAConcurrentChange() {
        routineBookingInThreeDays();

        assertThrows(ConcurrentBookingException.class,
                () -> service.confirmAppointment(new ConfirmAppointmentRequestDTO("BKG-1", 5L, PATIENT, ROLE)));
    }

    @Test
    void a7_confirmingACancelledAppointmentIsRejected() {
        routineBookingInThreeDays().cancel("Staff cancellation", "adm-1", NOW);

        assertThrows(RequestAlreadyProcessedException.class,
                () -> service.confirmAppointment(new ConfirmAppointmentRequestDTO("BKG-1", 2L, PATIENT, ROLE)));
        assertEquals(BookingStatus.CANCELLED, bookings.findById("BKG-1").orElseThrow().getStatus());
    }

    @Test
    void a8_aPatientCannotConfirmSomeoneElsesAppointment() {
        routineBookingInThreeDays();

        assertThrows(UnauthorizedBookingAccessException.class,
                () -> service.confirmAppointment(new ConfirmAppointmentRequestDTO("BKG-1", 1L, OTHER_PATIENT, ROLE)));
        assertEquals(BookingStatus.BOOKED, bookings.findById("BKG-1").orElseThrow().getStatus());
    }

    @Test
    void ifTheAuditCannotBeRecordedTheConfirmationIsNotApplied() {
        routineBookingInThreeDays();
        PatientAppointmentServiceImpl serviceWithBrokenAudit = new PatientAppointmentServiceImpl(bookings, requests,
                worklist, scheduleService, orders, new CatalogueWithAbdominalCt(), policy, notifications,
                (actorId, role) -> false,
                event -> { throw new IllegalStateException("audit store down"); },
                published::add, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(IllegalStateException.class, () -> serviceWithBrokenAudit.confirmAppointment(
                new ConfirmAppointmentRequestDTO("BKG-1", 1L, PATIENT, ROLE)));

        assertEquals(BookingStatus.BOOKED, bookings.findById("BKG-1").orElseThrow().getStatus());
        assertTrue(published.isEmpty());
    }

    // ---------------------------------------------------------------- A2: rescheduling request (+A6, A7)

    @Test
    void a2_aRescheduleRequestIsStoredAsPendingReviewAndTheAppointmentDoesNotChange() {
        routineBookingInThreeDays();
        List<LocalDate> preferred = List.of(LocalDate.of(2026, 12, 10));

        RescheduleRequestStatusDTO status = service.requestRescheduling(
                new PatientRescheduleRequestDTO("BKG-1", "Work trip", preferred, PATIENT, ROLE));

        assertEquals(RescheduleRequestStatus.PENDING_REVIEW, status.status());
        assertEquals(preferred, status.preferredDates());
        Booking stored = bookings.findById("BKG-1").orElseThrow();
        assertEquals(BookingStatus.BOOKED, stored.getStatus());
        assertEquals(1L, stored.getVersion());
        assertTrue(published.isEmpty());
        assertEquals(List.of("APPOINTMENT_RESCHEDULE_REQUESTED"), auditedActions());

        // The patient sees the request status, and cannot ask again while it is pending.
        AppointmentDetailsDTO details = service.getAppointmentDetails("BKG-1", PATIENT, ROLE);
        assertEquals(status.requestId(), details.pendingRequest().requestId());
        assertEquals(Set.of(AppointmentAction.CONFIRM, AppointmentAction.CANCEL), details.allowedActions());
    }

    @Test
    void a6_aSecondRescheduleRequestIsRejected() {
        routineBookingInThreeDays();
        service.requestRescheduling(new PatientRescheduleRequestDTO("BKG-1", "Work trip", null, PATIENT, ROLE));

        assertThrows(DuplicatePendingRequestException.class, () -> service.requestRescheduling(
                new PatientRescheduleRequestDTO("BKG-1", "Still travelling", null, PATIENT, ROLE)));
        assertEquals("APPOINTMENT_ACTION_REJECTED", auditedActions().get(auditedActions().size() - 1));
    }

    @Test
    void a2_aRescheduleRequestWithoutReasonIsInvalid() {
        routineBookingInThreeDays();

        InvalidBookingDataException ex = assertThrows(InvalidBookingDataException.class,
                () -> service.requestRescheduling(new PatientRescheduleRequestDTO("BKG-1", "", null, PATIENT, ROLE)));
        assertEquals(List.of("reason"), ex.getInvalidFields());
        assertTrue(requests.findPendingByBookingId("BKG-1").isEmpty());
    }

    @Test
    void a7_aCancelledAppointmentReportsItsStateEvenIfARequestWasStillPending() {
        Booking booking = routineBookingInThreeDays();
        service.requestRescheduling(new PatientRescheduleRequestDTO("BKG-1", "Work trip", null, PATIENT, ROLE));
        booking.cancel("Staff cancellation", "adm-1", NOW);

        assertThrows(RequestAlreadyProcessedException.class, () -> service.requestRescheduling(
                new PatientRescheduleRequestDTO("BKG-1", "Another date", null, PATIENT, ROLE)));
    }

    @Test
    void a7_anExecutedAppointmentCannotBeRescheduled() {
        routineBookingInThreeDays().markExecuted("SYSTEM", NOW);

        assertThrows(RequestAlreadyProcessedException.class, () -> service.requestRescheduling(
                new PatientRescheduleRequestDTO("BKG-1", "Work trip", null, PATIENT, ROLE)));
        assertTrue(requests.findPendingByBookingId("BKG-1").isEmpty());
    }

    // ---------------------------------------------------------------- A3, A4: cancel (+A5, A7)

    @Test
    void a3_cancellingReleasesTheSlotAndInformsAdmissionsAndThePatient() {
        Booking booking = routineBookingInThreeDays();
        orders.markOrderBooked(ROUTINE_ORDER);

        AppointmentDetailsDTO details = service.cancelAppointment(
                new CancelAppointmentRequestDTO("BKG-1", 1L, "Feeling better", false, PATIENT, ROLE));

        assertEquals(BookingStatus.CANCELLED, details.summary().status());
        assertTrue(details.allowedActions().isEmpty());
        verify(scheduleService).releaseSlot("SLOT-BKG-1", "BKG-1");
        assertTrue(orders.listPendingOrders().stream().anyMatch(o -> o.orderId().equals(ROUTINE_ORDER)),
                "the order goes back to pending, as in a staff cancellation (UC-AAD-05)");
        assertEquals(BookingStatus.CANCELLED,
                worklist.findByDate(booking.getScheduledStart().toLocalDate()).get(0).toDTO().status());
        assertEquals(List.of(BookingStatus.CANCELLED), notifications.sentStatuses);
        assertEquals(NotificationStatus.SENT, bookings.findById("BKG-1").orElseThrow().toDTO().notificationStatus());
        assertEquals(BookingStatus.CANCELLED, published.get(0).status());
        AuditEventDTO audit = lastAudit("APPOINTMENT_CANCELLED");
        assertTrue(audit.details().contains("Feeling better"));
    }

    @Test
    void a4_cancellingAnUrgentExaminationRecordsThePriorityAndTheWarningInTheAudit() {
        bookingOf("BKG-URG", URGENT_ORDER, PATIENT, NOW_LOCAL.plusDays(2));

        service.cancelAppointment(new CancelAppointmentRequestDTO("BKG-URG", 1L, null, true, PATIENT, ROLE));

        String details = lastAudit("APPOINTMENT_CANCELLED").details();
        assertTrue(details.contains("STAT_URGENT"), details);
        assertTrue(details.contains("priority warning acknowledged: true"), details);
    }

    @Test
    void a5_cancellingAfterTheCancellationDeadlineIsRejectedAndKeepsTheSlot() {
        policy.cancelHours = 48;
        bookingOf("BKG-1", ROUTINE_ORDER, PATIENT, NOW_LOCAL.plusDays(1));

        assertThrows(BookingConflictException.class, () -> service.cancelAppointment(
                new CancelAppointmentRequestDTO("BKG-1", 1L, "Feeling better", false, PATIENT, ROLE)));

        verify(scheduleService, never()).releaseSlot(anyString(), anyString());
        assertEquals(BookingStatus.BOOKED, bookings.findById("BKG-1").orElseThrow().getStatus());
    }

    @Test
    void a7_cancellingTwiceIsRejected() {
        routineBookingInThreeDays();
        service.cancelAppointment(new CancelAppointmentRequestDTO("BKG-1", 1L, null, false, PATIENT, ROLE));

        assertThrows(RequestAlreadyProcessedException.class, () -> service.cancelAppointment(
                new CancelAppointmentRequestDTO("BKG-1", 2L, null, false, PATIENT, ROLE)));
    }

    @Test
    void aNotificationFailureDoesNotUndoTheCancellation() {
        routineBookingInThreeDays();
        notifications.fail = true;

        service.cancelAppointment(new CancelAppointmentRequestDTO("BKG-1", 1L, null, false, PATIENT, ROLE));

        Booking stored = bookings.findById("BKG-1").orElseThrow();
        assertEquals(BookingStatus.CANCELLED, stored.getStatus());
        assertEquals(NotificationStatus.PENDING_RETRY, stored.toDTO().notificationStatus());
        assertTrue(auditedActions().contains("BOOKING_NOTIFICATION_FAILED"));
    }

    // ---------------------------------------------------------------- test doubles

    /** Booking policy with configurable deadlines (0 = no deadline, as the AAD-05 default). */
    private static final class TestPolicy implements BookingPolicyGateway {
        static final String CONTACT = "Radiology admissions: 925 000 000";
        int confirmHours = 0;
        int cancelHours = 0;

        @Override
        public int maxBookingsPerPatientPerDay() {
            return 3;
        }

        @Override
        public int minHoursBeforeCancellation() {
            return cancelHours;
        }

        @Override
        public int minHoursBeforeConfirmation() {
            return confirmHours;
        }

        @Override
        public String serviceContact() {
            return CONTACT;
        }
    }

    /** Notification stand-in that remembers what was sent and can simulate a failure. */
    private static final class RecordingNotifications implements BookingNotificationGateway {
        final List<BookingStatus> sentStatuses = new ArrayList<>();
        boolean fail = false;

        @Override
        public NotificationStatus sendBookingNotification(String bookingId, String patientId,
                                                          BookingStatus status, String message) {
            if (fail) {
                throw new IllegalStateException("SMS provider unavailable");
            }
            sentStatuses.add(status);
            return NotificationStatus.SENT;
        }
    }

    /** Catalogue (C2) that only knows the abdominal CT with contrast. */
    private static final class CatalogueWithAbdominalCt implements ExamTypeService {
        private final Map<String, ExamTypeDTO> types = Map.of("CT-ABD-C", new ExamTypeDTO("CT-ABD-C",
                "Abdominal CT with contrast", ModalityType.CT, 30, "Fast for 6 hours.",
                true, true, true, "CONSENT-CONTRAST", true));

        @Override
        public List<ExamTypeDTO> listActiveExamTypes() {
            return List.copyOf(types.values());
        }

        @Override
        public ExamTypeDTO getActiveExamType(String code) {
            ExamTypeDTO type = types.get(code);
            if (type == null) {
                throw new ExamTypeNotFoundException("Examination type '" + code + "' does not exist.");
            }
            return type;
        }
    }
}
