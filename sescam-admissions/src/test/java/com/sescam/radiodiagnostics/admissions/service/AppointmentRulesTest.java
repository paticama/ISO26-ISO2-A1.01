package com.sescam.radiodiagnostics.admissions.service;

import com.sescam.radiodiagnostics.admissions.domain.Booking;
import com.sescam.radiodiagnostics.common.admissions.AppointmentAction;
import com.sescam.radiodiagnostics.common.admissions.BookingPolicyGateway;
import com.sescam.radiodiagnostics.common.admissions.PatientRescheduleRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.sescam.radiodiagnostics.common.admissions.BookingErrorCode.BOOKING_CONFLICT;
import static com.sescam.radiodiagnostics.common.admissions.BookingErrorCode.CONCURRENT_BOOKING;
import static com.sescam.radiodiagnostics.common.admissions.BookingErrorCode.REQUEST_ALREADY_PROCESSED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Policy rules of UC-PAT-01: allowed actions (step 5), deadlines (A5), pending requests (A6), final states (A7). */
class AppointmentRulesTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 26, 10, 0);

    private static Booking bookingStartingAt(LocalDateTime start) {
        SlotDTO slot = new SlotDTO("SLOT-1", "SCH-1", "CT-1", "LOC-1", "CT", start, start.plusMinutes(30),
                1, 0, 1L, true);
        return new Booking("BKG-1", "ORD-1", "PAT-001", "CT", slot, "adm-1", Instant.parse("2026-11-20T10:00:00Z"));
    }

    private static AppointmentRules rulesWithDeadlines(int confirmHours, int cancelHours) {
        return new AppointmentRules(new BookingPolicyGateway() {
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
                return "Call 900 000 000";
            }
        });
    }

    private final AppointmentRules noDeadlines = rulesWithDeadlines(0, 0);

    @Test
    void aBookedFutureAppointmentAllowsEveryAction() {
        Booking booking = bookingStartingAt(NOW.plusDays(3));

        assertEquals(Set.of(AppointmentAction.CONFIRM, AppointmentAction.REQUEST_RESCHEDULE, AppointmentAction.CANCEL),
                noDeadlines.allowedActions(booking, false, NOW));
    }

    @Test
    void aConfirmedAppointmentCanBeMovedOrCancelledButNotConfirmedAgain() {
        Booking booking = bookingStartingAt(NOW.plusDays(3));
        booking.confirm("PAT-001", Instant.parse("2026-11-26T09:00:00Z"));

        assertEquals(Set.of(AppointmentAction.REQUEST_RESCHEDULE, AppointmentAction.CANCEL),
                noDeadlines.allowedActions(booking, false, NOW));
        assertEquals(Optional.of(REQUEST_ALREADY_PROCESSED), noDeadlines.checkConfirm(booking, booking.getVersion(), NOW));
    }

    @Test
    void cancelledAndExecutedAppointmentsAllowNothing() {
        Booking cancelled = bookingStartingAt(NOW.plusDays(3));
        cancelled.cancel("Patient request", "PAT-001", Instant.parse("2026-11-26T09:00:00Z"));
        Booking executed = bookingStartingAt(NOW.plusDays(3));
        executed.markExecuted("SYSTEM", Instant.parse("2026-11-26T09:00:00Z"));

        for (Booking booking : List.of(cancelled, executed)) {
            assertTrue(noDeadlines.allowedActions(booking, false, NOW).isEmpty());
            assertEquals(Optional.of(REQUEST_ALREADY_PROCESSED), noDeadlines.checkConfirm(booking, booking.getVersion(), NOW));
            assertEquals(Optional.of(REQUEST_ALREADY_PROCESSED), noDeadlines.checkCancel(booking, booking.getVersion(), NOW));
            assertEquals(Optional.of(REQUEST_ALREADY_PROCESSED), noDeadlines.checkReschedule(booking, NOW));
        }
    }

    @Test
    void aPendingRequestRemovesTheRescheduleAction() {
        Booking booking = bookingStartingAt(NOW.plusDays(3));

        assertEquals(Set.of(AppointmentAction.CONFIRM, AppointmentAction.CANCEL),
                noDeadlines.allowedActions(booking, true, NOW));
    }

    @Test
    void confirmationIsAllowedUntilTheConfiguredDeadlineIncluded() {
        AppointmentRules rules = rulesWithDeadlines(24, 0);
        Booking exactlyAtDeadline = bookingStartingAt(NOW.plusHours(24));
        Booking afterDeadline = bookingStartingAt(NOW.plusHours(23));

        assertEquals(Optional.empty(), rules.checkConfirm(exactlyAtDeadline, 1L, NOW));
        assertEquals(Optional.of(BOOKING_CONFLICT), rules.checkConfirm(afterDeadline, 1L, NOW));
        assertEquals(Set.of(AppointmentAction.REQUEST_RESCHEDULE, AppointmentAction.CANCEL),
                rules.allowedActions(afterDeadline, false, NOW));
    }

    @Test
    void cancellationOutsideItsWindowIsAConflict() {
        AppointmentRules rules = rulesWithDeadlines(0, 48);
        Booking booking = bookingStartingAt(NOW.plusHours(47));

        assertEquals(Optional.of(BOOKING_CONFLICT), rules.checkCancel(booking, 1L, NOW));
        assertEquals(Set.of(AppointmentAction.CONFIRM, AppointmentAction.REQUEST_RESCHEDULE),
                rules.allowedActions(booking, false, NOW));
    }

    @Test
    void anAppointmentThatAlreadyStartedCannotBeChanged() {
        Booking booking = bookingStartingAt(NOW.minusMinutes(5));

        assertTrue(noDeadlines.allowedActions(booking, false, NOW).isEmpty());
        assertEquals(Optional.of(BOOKING_CONFLICT), noDeadlines.checkConfirm(booking, 1L, NOW));
        assertEquals(Optional.of(BOOKING_CONFLICT), noDeadlines.checkCancel(booking, 1L, NOW));
        assertEquals(Optional.of(BOOKING_CONFLICT), noDeadlines.checkReschedule(booking, NOW));
    }

    @Test
    void aStaleVersionIsAConcurrentChange() {
        Booking booking = bookingStartingAt(NOW.plusDays(3));

        assertEquals(Optional.of(CONCURRENT_BOOKING), noDeadlines.checkConfirm(booking, 0L, NOW));
        assertEquals(Optional.of(CONCURRENT_BOOKING), noDeadlines.checkCancel(booking, 7L, NOW));
    }

    @Test
    void theStateIsReportedBeforeAStaleVersion() {
        Booking booking = bookingStartingAt(NOW.plusDays(3));
        booking.cancel("Staff cancellation", "adm-1", Instant.parse("2026-11-26T09:00:00Z"));

        assertEquals(Optional.of(REQUEST_ALREADY_PROCESSED), noDeadlines.checkCancel(booking, 1L, NOW));
    }

    @Test
    void aRescheduleRequestNeedsAReasonAndNoPastDates() {
        LocalDate today = NOW.toLocalDate();

        assertEquals(List.of(), noDeadlines.invalidRescheduleFields(
                new PatientRescheduleRequestDTO("BKG-1", "Work trip", List.of(today, today.plusDays(5)), "PAT-001", "PATIENT"),
                today));
        assertEquals(List.of("reason"), noDeadlines.invalidRescheduleFields(
                new PatientRescheduleRequestDTO("BKG-1", "  ", List.of(), "PAT-001", "PATIENT"), today));
        assertEquals(List.of("preferredDates"), noDeadlines.invalidRescheduleFields(
                new PatientRescheduleRequestDTO("BKG-1", "Work trip", List.of(today.minusDays(1)), "PAT-001", "PATIENT"),
                today));
    }

    @Test
    void theServiceContactComesFromThePolicy() {
        assertEquals("Call 900 000 000", noDeadlines.serviceContact());
    }
}
