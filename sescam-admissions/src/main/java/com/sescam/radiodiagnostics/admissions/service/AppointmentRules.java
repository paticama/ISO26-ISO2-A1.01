package com.sescam.radiodiagnostics.admissions.service;

import com.sescam.radiodiagnostics.admissions.domain.Booking;
import com.sescam.radiodiagnostics.common.admissions.AppointmentAction;
import com.sescam.radiodiagnostics.common.admissions.BookingErrorCode;
import com.sescam.radiodiagnostics.common.admissions.BookingPolicyGateway;
import com.sescam.radiodiagnostics.common.admissions.BookingStatus;
import com.sescam.radiodiagnostics.common.admissions.PatientRescheduleRequestDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Booking-policy rules for the patient's actions (UC-PAT-01).
 *
 * <p>Each check returns the error code that blocks the action, or empty if the action is allowed.
 * The checks are ordered from the most to the least informative for the patient:
 * state of the appointment (A7), then a concurrent change, then the policy deadline (A5).
 * {@link #allowedActions} uses the same checks, so the buttons shown always match what the server accepts.</p>
 */
final class AppointmentRules {

    /** States in which the patient may still confirm attendance. */
    private static final Set<BookingStatus> CONFIRMABLE = EnumSet.of(BookingStatus.BOOKED, BookingStatus.RESCHEDULED);

    /** States in which the patient may still cancel or ask for another date. CANCELLED and EXECUTED are final. */
    private static final Set<BookingStatus> CHANGEABLE =
            EnumSet.of(BookingStatus.BOOKED, BookingStatus.RESCHEDULED, BookingStatus.CONFIRMED);

    private final BookingPolicyGateway policy;

    AppointmentRules(BookingPolicyGateway policy) {
        this.policy = Objects.requireNonNull(policy);
    }

    /** Actions the patient may take now (step 5). A pending rescheduling request hides REQUEST_RESCHEDULE (A6). */
    Set<AppointmentAction> allowedActions(Booking booking, boolean hasPendingRequest, LocalDateTime now) {
        Set<AppointmentAction> actions = EnumSet.noneOf(AppointmentAction.class);
        if (checkConfirm(booking, booking.getVersion(), now).isEmpty()) {
            actions.add(AppointmentAction.CONFIRM);
        }
        if (!hasPendingRequest && checkReschedule(booking, now).isEmpty()) {
            actions.add(AppointmentAction.REQUEST_RESCHEDULE);
        }
        if (checkCancel(booking, booking.getVersion(), now).isEmpty()) {
            actions.add(AppointmentAction.CANCEL);
        }
        return actions;
    }

    Optional<BookingErrorCode> checkConfirm(Booking booking, long expectedVersion, LocalDateTime now) {
        return checkChange(booking, CONFIRMABLE, expectedVersion, policy.minHoursBeforeConfirmation(), now);
    }

    Optional<BookingErrorCode> checkCancel(Booking booking, long expectedVersion, LocalDateTime now) {
        return checkChange(booking, CHANGEABLE, expectedVersion, policy.minHoursBeforeCancellation(), now);
    }

    /**
     * A rescheduling request does not change the booking, so it has no version check and no policy deadline:
     * the appointment only has to be active and not started yet.
     */
    Optional<BookingErrorCode> checkReschedule(Booking booking, LocalDateTime now) {
        if (!CHANGEABLE.contains(booking.getStatus())) {
            return Optional.of(BookingErrorCode.REQUEST_ALREADY_PROCESSED);
        }
        if (!isBeforeDeadline(booking.getScheduledStart(), 0, now)) {
            return Optional.of(BookingErrorCode.BOOKING_CONFLICT);
        }
        return Optional.empty();
    }

    /** Names of the invalid fields of a rescheduling request (A2): the reason is mandatory, dates cannot be past. */
    List<String> invalidRescheduleFields(PatientRescheduleRequestDTO request, LocalDate today) {
        List<String> invalid = new ArrayList<>();
        if (request.reason() == null || request.reason().isBlank()) {
            invalid.add("reason");
        }
        boolean pastOrMissingDate = request.preferredDates().stream()
                .anyMatch(date -> date == null || date.isBefore(today));
        if (pastOrMissingDate) {
            invalid.add("preferredDates");
        }
        return invalid;
    }

    String serviceContact() {
        return policy.serviceContact();
    }

    private Optional<BookingErrorCode> checkChange(Booking booking, Set<BookingStatus> allowedStates,
                                                   long expectedVersion, int minHoursBefore, LocalDateTime now) {
        if (!allowedStates.contains(booking.getStatus())) {
            return Optional.of(BookingErrorCode.REQUEST_ALREADY_PROCESSED);
        }
        if (booking.getVersion() != expectedVersion) {
            return Optional.of(BookingErrorCode.CONCURRENT_BOOKING);
        }
        if (!isBeforeDeadline(booking.getScheduledStart(), minHoursBefore, now)) {
            return Optional.of(BookingErrorCode.BOOKING_CONFLICT);
        }
        return Optional.empty();
    }

    /**
     * True if the appointment has not started and at least {@code minHoursBefore} full hours remain until it,
     * the same way UC-AAD-05 measures its cancellation window.
     */
    private static boolean isBeforeDeadline(LocalDateTime start, int minHoursBefore, LocalDateTime now) {
        return now.isBefore(start) && ChronoUnit.HOURS.between(now, start) >= minHoursBefore;
    }
}
