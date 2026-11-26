package com.sescam.radiodiagnostics.admissions.service;

import com.sescam.radiodiagnostics.admissions.internal.DefaultBookingPolicyGateway;
import com.sescam.radiodiagnostics.admissions.internal.DefaultPatientEligibilityGateway;
import com.sescam.radiodiagnostics.admissions.internal.InMemoryPendingOrderGateway;
import com.sescam.radiodiagnostics.admissions.internal.NoOpBookingEventPublisher;
import com.sescam.radiodiagnostics.admissions.internal.NoOpBookingNotificationGateway;
import com.sescam.radiodiagnostics.admissions.repository.InMemoryBookingRepository;
import com.sescam.radiodiagnostics.admissions.repository.InMemoryWorklistRepository;
import com.sescam.radiodiagnostics.common.admissions.BookingConflictException;
import com.sescam.radiodiagnostics.common.admissions.BookingDTO;
import com.sescam.radiodiagnostics.common.admissions.BookingEventPublisher;
import com.sescam.radiodiagnostics.common.admissions.BookingNotFoundException;
import com.sescam.radiodiagnostics.common.admissions.BookingNotificationGateway;
import com.sescam.radiodiagnostics.common.admissions.BookingPolicyGateway;
import com.sescam.radiodiagnostics.common.admissions.BookingStatus;
import com.sescam.radiodiagnostics.common.admissions.CancelBookingRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.ConcurrentBookingException;
import com.sescam.radiodiagnostics.common.admissions.CreateBookingRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.InvalidBookingDataException;
import com.sescam.radiodiagnostics.common.admissions.NotificationStatus;
import com.sescam.radiodiagnostics.common.admissions.PatientEligibilityGateway;
import com.sescam.radiodiagnostics.common.admissions.PatientNotEligibleException;
import com.sescam.radiodiagnostics.common.admissions.PendingExamRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.RequestAlreadyProcessedException;
import com.sescam.radiodiagnostics.common.admissions.RescheduleBookingRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.SlotUnavailableBookingException;
import com.sescam.radiodiagnostics.common.admissions.UnauthorizedBookingAccessException;
import com.sescam.radiodiagnostics.common.orders.PriorityLevel;
import com.sescam.radiodiagnostics.common.schedules.ConcurrentScheduleChangeException;
import com.sescam.radiodiagnostics.common.schedules.ScheduleNotFoundException;
import com.sescam.radiodiagnostics.common.schedules.ScheduleService;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import com.sescam.radiodiagnostics.common.schedules.SlotUnavailableException;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private ScheduleService scheduleService;
    @Mock
    private AuditService auditService;
    @Mock
    private BookingEventPublisher eventPublisher;

    private InMemoryBookingRepository bookingRepository;
    private InMemoryWorklistRepository worklistRepository;
    private InMemoryPendingOrderGateway orderGateway;
    private AccessControlService accessControlService;
    private BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        bookingRepository = new InMemoryBookingRepository();
        worklistRepository = new InMemoryWorklistRepository();
        orderGateway = new InMemoryPendingOrderGateway();
        accessControlService = (actorId, role) -> false; // use default canManageBookings
        service = buildService(new DefaultPatientEligibilityGateway(),
                new DefaultBookingPolicyGateway(),
                new NoOpBookingNotificationGateway());
    }

    private BookingServiceImpl buildService(PatientEligibilityGateway eligibility,
                                            BookingPolicyGateway policy,
                                            BookingNotificationGateway notification) {
        return new BookingServiceImpl(bookingRepository, worklistRepository, scheduleService,
                orderGateway, eligibility, policy, notification,
                accessControlService, auditService, eventPublisher, FIXED_CLOCK);
    }

    private static SlotDTO slot(String slotId, LocalDateTime start, long version) {
        return new SlotDTO(slotId, "SCH-1", "CT-1", "LOC-1", "CT",
                start, start.plusMinutes(30), 2, 0, version, true);
    }

    @Test
    void listPendingRequestsOrdersByPriorityThenTime() {
        orderGateway.seed(new PendingExamRequestDTO("ORD-999", "PHY-2026-0999", "PAT-009", "CT-ABD",
                "Abdominal CT", "CT", PriorityLevel.ROUTINE, Instant.parse("2026-11-25T08:00:00Z")));

        List<PendingExamRequestDTO> pending = service.listPendingRequests("adm-1", "ADMISSIONS_STAFF");
        assertEquals(3, pending.size());
        assertEquals(PriorityLevel.STAT_URGENT, pending.get(0).priority());
        assertEquals(PriorityLevel.PREFERENTIAL, pending.get(1).priority());
        assertEquals(PriorityLevel.ROUTINE, pending.get(2).priority());
    }

    @Test
    void createBookingSucceedsUpdatesWorklistMarksOrderBookedAndAudits() {
        SlotDTO s1 = slot("SLOT-1", LocalDateTime.of(2026, 11, 27, 9, 0), 1L);
        when(scheduleService.reserveSlotAtomically(eq("SLOT-1"), eq(1L), anyString())).thenReturn(s1);

        BookingDTO created = service.createBooking(
                new CreateBookingRequestDTO("ORD-1001", "SLOT-1", 1L, "adm-1", "ADMISSIONS_STAFF"));

        assertEquals(BookingStatus.BOOKED, created.status());
        assertEquals(NotificationStatus.SENT, created.notificationStatus());
        assertEquals("SLOT-1", created.slotId());
        assertEquals(1, created.history().size());
        assertEquals(1, service.listDailyWorklist(LocalDate.of(2026, 11, 27), "adm-1", "ADMISSIONS_STAFF").size());
        assertTrue(service.listPendingRequests("adm-1", "ADMISSIONS_STAFF").stream()
                .noneMatch(o -> o.orderId().equals("ORD-1001")));
        verify(auditService).recordEvent(any());
        verify(eventPublisher).publish(any());
    }

    @Test
    void findCompatibleSlotsDelegatesToScheduleServiceForOrderModality() {
        SlotDTO s1 = slot("SLOT-1", LocalDateTime.of(2026, 11, 27, 9, 0), 1L);
        when(scheduleService.findCompatibleSlots(eq("CT"), any(), any())).thenReturn(List.of(s1));

        List<SlotDTO> found = service.findCompatibleSlots("ORD-1001", null, null, "adm-1", "ADMISSIONS_STAFF");
        assertEquals(1, found.size());
        assertEquals("SLOT-1", found.get(0).slotId());
    }

    @Test
    void a1OccupiedSlotThrowsSlotUnavailableWithAlternatives() {
        SlotDTO alt = slot("SLOT-ALT", LocalDateTime.of(2026, 11, 27, 10, 0), 1L);
        when(scheduleService.reserveSlotAtomically(eq("SLOT-1"), eq(1L), anyString()))
                .thenThrow(new SlotUnavailableException("Slot full"));
        when(scheduleService.findCompatibleSlots(eq("CT"), any(), any())).thenReturn(List.of(alt));

        SlotUnavailableBookingException ex = assertThrows(SlotUnavailableBookingException.class,
                () -> service.createBooking(new CreateBookingRequestDTO("ORD-1001", "SLOT-1", 1L, "adm-1", "ADMISSIONS_STAFF")));
        assertEquals(1, ex.getAlternativeSlots().size());
        assertEquals("SLOT-ALT", ex.getAlternativeSlots().get(0).slotId());
    }

    @Test
    void a2ReschedulePreservesHistoryReleasesOldSlotAndUpdatesWorklist() {
        SlotDTO s1 = slot("SLOT-1", LocalDateTime.of(2026, 11, 27, 9, 0), 1L);
        SlotDTO s2 = slot("SLOT-2", LocalDateTime.of(2026, 11, 28, 11, 0), 1L);
        when(scheduleService.reserveSlotAtomically(eq("SLOT-1"), eq(1L), anyString())).thenReturn(s1);
        when(scheduleService.reserveSlotAtomically(eq("SLOT-2"), eq(1L), anyString())).thenReturn(s2);

        BookingDTO created = service.createBooking(
                new CreateBookingRequestDTO("ORD-1001", "SLOT-1", 1L, "adm-1", "ADMISSIONS_STAFF"));

        BookingDTO rescheduled = service.rescheduleBooking(new RescheduleBookingRequestDTO(
                created.bookingId(), "SLOT-2", 1L, created.version(), "Patient requested morning", "adm-1", "ADMISSIONS_STAFF"));

        assertEquals(BookingStatus.RESCHEDULED, rescheduled.status());
        assertEquals("SLOT-2", rescheduled.slotId());
        assertEquals(2, rescheduled.history().size());
        verify(scheduleService).releaseSlot("SLOT-1", created.bookingId());
        assertEquals(1, service.listDailyWorklist(LocalDate.of(2026, 11, 28), "adm-1", "ADMISSIONS_STAFF").size());
    }

    @Test
    void a3CancelReleasesSlotReopensPendingOrderAndEnforcesPolicyWindow() {
        SlotDTO s1 = slot("SLOT-1", LocalDateTime.of(2026, 11, 27, 9, 0), 1L);
        when(scheduleService.reserveSlotAtomically(eq("SLOT-1"), eq(1L), anyString())).thenReturn(s1);
        BookingDTO created = service.createBooking(
                new CreateBookingRequestDTO("ORD-1001", "SLOT-1", 1L, "adm-1", "ADMISSIONS_STAFF"));

        BookingDTO cancelled = service.cancelBooking(new CancelBookingRequestDTO(
                created.bookingId(), created.version(), "Patient cancelled", "adm-1", "ADMISSIONS_STAFF"));

        assertEquals(BookingStatus.CANCELLED, cancelled.status());
        assertEquals(2, cancelled.history().size());
        verify(scheduleService).releaseSlot("SLOT-1", created.bookingId());
        assertTrue(service.listPendingRequests("adm-1", "ADMISSIONS_STAFF").stream()
                .anyMatch(o -> o.orderId().equals("ORD-1001")));

        BookingServiceImpl strictPolicy = buildService(new DefaultPatientEligibilityGateway(),
                new BookingPolicyGateway() {
                    @Override public int maxBookingsPerPatientPerDay() { return 3; }
                    @Override public int minHoursBeforeCancellation() { return 48; }
                }, new NoOpBookingNotificationGateway());
        BookingDTO second = strictPolicy.createBooking(
                new CreateBookingRequestDTO("ORD-1001", "SLOT-1", 1L, "adm-1", "ADMISSIONS_STAFF"));
        assertThrows(BookingConflictException.class, () -> strictPolicy.cancelBooking(
                new CancelBookingRequestDTO(second.bookingId(), second.version(), "Too late", "adm-1", "ADMISSIONS_STAFF")));
    }

    @Test
    void a4NotificationFailureKeepsBookingAuditableWithPendingRetry() {
        BookingServiceImpl failingNotifier = buildService(
                new DefaultPatientEligibilityGateway(),
                new DefaultBookingPolicyGateway(),
                (id, pat, status, msg) -> { throw new IllegalStateException("SMS gateway down"); });

        SlotDTO s1 = slot("SLOT-1", LocalDateTime.of(2026, 11, 27, 9, 0), 1L);
        when(scheduleService.reserveSlotAtomically(eq("SLOT-1"), eq(1L), anyString())).thenReturn(s1);

        BookingDTO created = failingNotifier.createBooking(
                new CreateBookingRequestDTO("ORD-1001", "SLOT-1", 1L, "adm-1", "ADMISSIONS_STAFF"));

        assertEquals(BookingStatus.BOOKED, created.status());
        assertEquals(NotificationStatus.PENDING_RETRY, created.notificationStatus());
        assertEquals(BookingStatus.BOOKED, failingNotifier.getBooking(created.bookingId(), "adm-1", "ADMISSIONS_STAFF").status());
    }

    @Test
    void a5ConcurrentBookingRejectsStaleSlotOrBookingVersion() {
        when(scheduleService.reserveSlotAtomically(eq("SLOT-1"), eq(1L), anyString()))
                .thenThrow(new ConcurrentScheduleChangeException("Stale slot"));
        assertThrows(ConcurrentBookingException.class, () -> service.createBooking(
                new CreateBookingRequestDTO("ORD-1001", "SLOT-1", 1L, "adm-1", "ADMISSIONS_STAFF")));

        SlotDTO s1 = slot("SLOT-1", LocalDateTime.of(2026, 11, 27, 9, 0), 2L);
        when(scheduleService.reserveSlotAtomically(eq("SLOT-1"), eq(2L), anyString())).thenReturn(s1);
        BookingDTO created = service.createBooking(
                new CreateBookingRequestDTO("ORD-1001", "SLOT-1", 2L, "adm-1", "ADMISSIONS_STAFF"));

        assertThrows(ConcurrentBookingException.class, () -> service.cancelBooking(
                new CancelBookingRequestDTO(created.bookingId(), 99L, "stale", "adm-1", "ADMISSIONS_STAFF")));
    }

    @Test
    void a6DeniesSystemAdministratorAndInvalidRequests() {
        assertThrows(UnauthorizedBookingAccessException.class,
                () -> service.listPendingRequests("admin-1", "SYSTEM_ADMINISTRATOR"));
        assertThrows(UnauthorizedBookingAccessException.class,
                () -> service.createBooking(new CreateBookingRequestDTO("ORD-1001", "SLOT-1", 1L, "", "ADMISSIONS_STAFF")));
        assertThrows(InvalidBookingDataException.class,
                () -> service.createBooking(new CreateBookingRequestDTO("", "", 1L, "adm-1", "ADMISSIONS_STAFF")));
        assertThrows(BookingNotFoundException.class,
                () -> service.createBooking(new CreateBookingRequestDTO("UNKNOWN", "SLOT-1", 1L, "adm-1", "ADMISSIONS_STAFF")));
    }

    @Test
    void rejectsIneligiblePatientDuplicateOrderDailyLimitAndUnknownSlot() {
        BookingServiceImpl ineligible = buildService(p -> false,
                new DefaultBookingPolicyGateway(), new NoOpBookingNotificationGateway());
        assertThrows(PatientNotEligibleException.class, () -> ineligible.createBooking(
                new CreateBookingRequestDTO("ORD-1001", "SLOT-1", 1L, "adm-1", "ADMISSIONS_STAFF")));

        when(scheduleService.reserveSlotAtomically(eq("NO-SLOT"), anyLong(), anyString()))
                .thenThrow(new ScheduleNotFoundException("Missing slot"));
        assertThrows(BookingNotFoundException.class, () -> service.createBooking(
                new CreateBookingRequestDTO("ORD-1001", "NO-SLOT", 1L, "adm-1", "ADMISSIONS_STAFF")));

        SlotDTO s1 = slot("SLOT-1", LocalDateTime.of(2026, 11, 27, 9, 0), 1L);
        when(scheduleService.reserveSlotAtomically(eq("SLOT-1"), eq(1L), anyString())).thenReturn(s1);
        service.createBooking(new CreateBookingRequestDTO("ORD-1001", "SLOT-1", 1L, "adm-1", "ADMISSIONS_STAFF"));

        assertThrows(RequestAlreadyProcessedException.class, () -> service.createBooking(
                new CreateBookingRequestDTO("ORD-1001", "SLOT-1", 1L, "adm-1", "ADMISSIONS_STAFF")));

        // Daily limit = 1 for same patient
        BookingServiceImpl onePerDay = buildService(new DefaultPatientEligibilityGateway(),
                new BookingPolicyGateway() {
                    @Override public int maxBookingsPerPatientPerDay() { return 1; }
                    @Override public int minHoursBeforeCancellation() { return 0; }
                }, new NoOpBookingNotificationGateway());
        orderGateway.seed(new PendingExamRequestDTO("ORD-1003", "PHY-2026-1003", "PAT-001", "CT-BRAIN",
                "Brain CT", "CT", PriorityLevel.ROUTINE, NOW));
        assertThrows(BookingConflictException.class, () -> onePerDay.createBooking(
                new CreateBookingRequestDTO("ORD-1003", "SLOT-1", 1L, "adm-1", "ADMISSIONS_STAFF")));
    }

    @Test
    void releasesReservedSlotIfAuditOrPersistenceFails() {
        SlotDTO s1 = slot("SLOT-1", LocalDateTime.of(2026, 11, 27, 9, 0), 1L);
        when(scheduleService.reserveSlotAtomically(eq("SLOT-1"), eq(1L), anyString())).thenReturn(s1);
        doThrow(new IllegalStateException("audit unavailable")).when(auditService).recordEvent(any());

        assertThrows(IllegalStateException.class, () -> service.createBooking(
                new CreateBookingRequestDTO("ORD-1001", "SLOT-1", 1L, "adm-1", "ADMISSIONS_STAFF")));
    }
}
