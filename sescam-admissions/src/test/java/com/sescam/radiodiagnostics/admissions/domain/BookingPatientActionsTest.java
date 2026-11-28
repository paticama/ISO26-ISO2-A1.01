package com.sescam.radiodiagnostics.admissions.domain;

import com.sescam.radiodiagnostics.common.admissions.BookingHistoryEntryDTO;
import com.sescam.radiodiagnostics.common.admissions.BookingStatus;
import com.sescam.radiodiagnostics.common.admissions.RequestAlreadyProcessedException;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Changes that UC-PAT-01 adds to the Booking aggregate of UC-AAD-05. */
class BookingPatientActionsTest {

    private static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");
    private static final LocalDateTime START = LocalDateTime.of(2026, 12, 3, 9, 0);

    private static Booking bookingOf(String patientId) {
        SlotDTO slot = new SlotDTO("SLOT-1", "SCH-1", "CT-1", "LOC-1", "CT", START, START.plusMinutes(30),
                1, 0, 1L, true);
        return new Booking("BKG-1", "ORD-1", patientId, "CT", slot, "adm-1", NOW.minusSeconds(3600));
    }

    @Test
    void confirmMovesABookedAppointmentToConfirmedAndKeepsHistory() {
        Booking booking = bookingOf("PAT-001");

        booking.confirm("PAT-001", NOW);

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(2L, booking.getVersion());
        List<BookingHistoryEntryDTO> history = booking.toDTO().history();
        BookingHistoryEntryDTO last = history.get(history.size() - 1);
        assertEquals(BookingStatus.BOOKED, last.previousStatus());
        assertEquals(BookingStatus.CONFIRMED, last.newStatus());
        assertEquals("PAT-001", last.changedBy());
        assertEquals(NOW, last.changedAt());
    }

    @Test
    void confirmAlsoAcceptsARescheduledAppointment() {
        Booking booking = bookingOf("PAT-001");
        booking.reschedule(new SlotDTO("SLOT-2", "SCH-1", "CT-1", "LOC-1", "CT", START.plusDays(1),
                START.plusDays(1).plusMinutes(30), 1, 0, 1L, true), "Moved by staff", "adm-1", NOW);

        booking.confirm("PAT-001", NOW);

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    void confirmingTwiceIsRejectedAndChangesNothing() {
        Booking booking = bookingOf("PAT-001");
        booking.confirm("PAT-001", NOW);

        assertThrows(RequestAlreadyProcessedException.class, () -> booking.confirm("PAT-001", NOW));
        assertEquals(2L, booking.getVersion());
    }

    @Test
    void aConfirmedAppointmentCanStillBeCancelled() {
        Booking booking = bookingOf("PAT-001");
        booking.confirm("PAT-001", NOW);

        booking.cancel("Cannot attend", "PAT-001", NOW);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    void anExecutedAppointmentCanNeitherBeCancelledNorConfirmed() {
        Booking booking = bookingOf("PAT-001");
        booking.markExecuted("SYSTEM", NOW);

        assertEquals(BookingStatus.EXECUTED, booking.getStatus());
        assertThrows(RequestAlreadyProcessedException.class, () -> booking.cancel("late", "PAT-001", NOW));
        assertThrows(RequestAlreadyProcessedException.class, () -> booking.confirm("PAT-001", NOW));
    }

    @Test
    void ownershipIsCheckedAgainstThePatientOfTheBooking() {
        Booking booking = bookingOf("PAT-001");

        assertTrue(booking.isOwnedBy("PAT-001"));
        assertTrue(booking.isOwnedBy(" PAT-001 "));
        assertFalse(booking.isOwnedBy("PAT-002"));
        assertFalse(booking.isOwnedBy(null));
    }
}
