package com.sescam.radiodiagnostics.common.admissions;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contracts added to C5 by UC-PAT-01 (Manage my appointments). */
class PatientAppointmentContractsTest {

    @Test
    void bookingStatusNamesUsedInJsonIncludeThePatientStates() {
        // The client receives these names as text in the JSON, so they must not be renamed.
        assertEquals(BookingStatus.CONFIRMED, BookingStatus.valueOf("CONFIRMED"));
        assertEquals(BookingStatus.EXECUTED, BookingStatus.valueOf("EXECUTED"));
        assertEquals(RescheduleRequestStatus.PENDING_REVIEW, RescheduleRequestStatus.valueOf("PENDING_REVIEW"));
    }

    @Test
    void duplicatePendingRequestExceptionCarriesItsOwnErrorCode() {
        var ex = new DuplicatePendingRequestException("already pending");

        assertEquals(BookingErrorCode.DUPLICATE_PENDING_REQUEST, ex.getCode());
        assertEquals("already pending", ex.getMessage());
    }

    @Test
    void policyGatewayDefaultsDoNotRestrictConfirmationAndGiveAServiceContact() {
        BookingPolicyGateway policy = new BookingPolicyGateway() {
            @Override
            public int maxBookingsPerPatientPerDay() {
                return 3;
            }

            @Override
            public int minHoursBeforeCancellation() {
                return 0;
            }
        };

        assertEquals(0, policy.minHoursBeforeConfirmation());
        assertFalse(policy.serviceContact().isBlank());
    }

    @Test
    void rescheduleRequestWithoutPreferredDatesHasAnEmptyList() {
        var request = new PatientRescheduleRequestDTO("BKG-1", "Work trip", null, "PAT-001", "PATIENT");

        assertTrue(request.preferredDates().isEmpty());
    }

    @Test
    void rescheduleRequestKeepsACopyOfThePreferredDates() {
        List<LocalDate> dates = new ArrayList<>(List.of(LocalDate.of(2026, 12, 10)));
        var request = new PatientRescheduleRequestDTO("BKG-1", "Work trip", dates, "PAT-001", "PATIENT");

        dates.add(LocalDate.of(2026, 12, 11));

        assertEquals(1, request.preferredDates().size());
        assertThrows(UnsupportedOperationException.class,
                () -> request.preferredDates().add(LocalDate.of(2026, 12, 12)));
    }

    @Test
    void rescheduleRequestStatusWithoutPreferredDatesHasAnEmptyList() {
        var status = new RescheduleRequestStatusDTO("REQ-1", "BKG-1", RescheduleRequestStatus.PENDING_REVIEW,
                "Work trip", null, null, null);

        assertTrue(status.preferredDates().isEmpty());
    }

    @Test
    void detailsWithoutAllowedActionsHaveAnEmptyImmutableSet() {
        var details = new AppointmentDetailsDTO(null, null, null, null, null, null, "contact", 1L);

        assertTrue(details.allowedActions().isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> details.allowedActions().add(AppointmentAction.CONFIRM));
    }

    @Test
    void detailsKeepACopyOfTheAllowedActions() {
        Set<AppointmentAction> actions = EnumSet.of(AppointmentAction.CONFIRM);
        var details = new AppointmentDetailsDTO(null, null, null, null, actions, null, "contact", 1L);

        actions.add(AppointmentAction.CANCEL);

        assertEquals(Set.of(AppointmentAction.CONFIRM), details.allowedActions());
    }
}
