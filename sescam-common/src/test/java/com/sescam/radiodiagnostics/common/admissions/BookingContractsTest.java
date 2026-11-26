package com.sescam.radiodiagnostics.common.admissions;

import com.sescam.radiodiagnostics.common.orders.PriorityLevel;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingContractsTest {

    @Test
    void exceptionsCarryExpectedErrorCodesAndPayloads() {
        var invalid = new InvalidBookingDataException("bad", List.of("orderId"));
        assertEquals(BookingErrorCode.INVALID_BOOKING_DATA, invalid.getCode());
        assertEquals(List.of("orderId"), invalid.getInvalidFields());
        assertTrue(new InvalidBookingDataException("bad", null).getInvalidFields().isEmpty());

        assertEquals(BookingErrorCode.UNAUTHORIZED_BOOKING_ACCESS,
                new UnauthorizedBookingAccessException("denied").getCode());
        assertEquals(BookingErrorCode.BOOKING_NOT_FOUND,
                new BookingNotFoundException("missing").getCode());
        assertEquals(BookingErrorCode.REQUEST_ALREADY_PROCESSED,
                new RequestAlreadyProcessedException("booked").getCode());
        assertEquals(BookingErrorCode.PATIENT_NOT_ELIGIBLE,
                new PatientNotEligibleException("discharged").getCode());
        assertEquals(BookingErrorCode.BOOKING_CONFLICT,
                new BookingConflictException("conflict").getCode());
        assertEquals(BookingErrorCode.CONCURRENT_BOOKING,
                new ConcurrentBookingException("stale").getCode());

        var slot = new SlotDTO("S1", "SCH-1", "CT-1", "LOC-1", "CT",
                LocalDateTime.of(2026, 12, 1, 9, 0), LocalDateTime.of(2026, 12, 1, 9, 30),
                2, 0, 1L, true);
        var unavailable = new SlotUnavailableBookingException("full", List.of(slot));
        assertEquals(BookingErrorCode.SLOT_UNAVAILABLE, unavailable.getCode());
        assertEquals(1, unavailable.getAlternativeSlots().size());
        assertTrue(new SlotUnavailableBookingException("full", null).getAlternativeSlots().isEmpty());
    }

    @Test
    void dtosEnumsAndAccessControlDefaultCoverAllBranches() {
        assertEquals(4, BookingStatus.values().length);
        assertEquals(3, NotificationStatus.values().length);
        assertEquals(8, BookingErrorCode.values().length);

        Instant now = Instant.parse("2026-11-26T10:00:00Z");
        LocalDateTime start = LocalDateTime.of(2026, 12, 1, 9, 0);
        var history = new BookingHistoryEntryDTO("E1", BookingStatus.PENDING, BookingStatus.BOOKED,
                null, "S1", "init", "adm-1", now);
        var booking = new BookingDTO("B1", "O1", "P1", "CT", "S1", "R1", "L1",
                start, start.plusMinutes(30), BookingStatus.BOOKED, NotificationStatus.SENT, 1L, List.of(history));
        assertEquals("B1", booking.bookingId());

        var pending = new PendingExamRequestDTO("O1", "N1", "P1", "CT-1", "Chest CT", "CT",
                PriorityLevel.STAT_URGENT, now);
        assertEquals("O1", pending.orderId());

        var worklist = new WorklistItemDTO("W1", "B1", "O1", "P1", "R1", "CT",
                start, start.plusMinutes(30), BookingStatus.BOOKED);
        assertEquals("W1", worklist.worklistId());

        var create = new CreateBookingRequestDTO("O1", "S1", 1L, "adm-1", "ADMISSIONS_STAFF");
        var reschedule = new RescheduleBookingRequestDTO("B1", "S2", 1L, 1L, "move", "adm-1", "ADMISSIONS_STAFF");
        var cancel = new CancelBookingRequestDTO("B1", 1L, "cancel", "adm-1", "ADMISSIONS_STAFF");
        var event = new BookingChangedEvent("B1", "O1", "P1", "S1", BookingStatus.BOOKED, start, now);
        assertEquals("O1", create.orderId());
        assertEquals("S2", reschedule.newSlotId());
        assertEquals("cancel", cancel.reason());
        assertEquals("B1", event.bookingId());

        AccessControlService rbac = (actorId, role) -> false;
        assertTrue(rbac.canManageBookings("adm-1", "ADMISSIONS_STAFF"));
        assertTrue(rbac.canManageBookings("adm-1", "RECEPTIONIST"));
        assertFalse(rbac.canManageBookings("adm-1", "SYSTEM_ADMINISTRATOR"));
        assertFalse(rbac.canManageBookings("", "ADMISSIONS_STAFF"));
        assertFalse(rbac.canManageBookings("adm-1", null));
    }
}
