package com.sescam.radiodiagnostics.admissions.domain;

import com.sescam.radiodiagnostics.common.admissions.RescheduleRequestStatus;
import com.sescam.radiodiagnostics.common.admissions.RescheduleRequestStatusDTO;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * A patient's request to move an appointment (UC-PAT-01 A2). It never changes the booking itself:
 * Admissions Staff review it and, if they accept it, reschedule the booking in UC-AAD-05.
 */
public class RescheduleRequest {

    private final String requestId;
    private final String bookingId;
    private final String patientId;
    private final String reason;
    private final List<LocalDate> preferredDates;
    private final RescheduleRequestStatus status;
    private final Instant requestedAt;
    private final Instant reviewedAt;

    private RescheduleRequest(String requestId, String bookingId, String patientId, String reason,
                              List<LocalDate> preferredDates, RescheduleRequestStatus status,
                              Instant requestedAt, Instant reviewedAt) {
        this.requestId = Objects.requireNonNull(requestId);
        this.bookingId = Objects.requireNonNull(bookingId);
        this.patientId = Objects.requireNonNull(patientId);
        this.reason = Objects.requireNonNull(reason);
        this.preferredDates = preferredDates == null ? List.of() : List.copyOf(preferredDates);
        this.status = Objects.requireNonNull(status);
        this.requestedAt = Objects.requireNonNull(requestedAt);
        this.reviewedAt = reviewedAt;
    }

    /** New request waiting for Admissions (PENDING_REVIEW). */
    public static RescheduleRequest pending(String bookingId, String patientId, String reason,
                                            List<LocalDate> preferredDates, Instant now) {
        String requestId = "RRQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return new RescheduleRequest(requestId, bookingId, patientId, reason, preferredDates,
                RescheduleRequestStatus.PENDING_REVIEW, now, null);
    }

    public boolean isPending() {
        return status == RescheduleRequestStatus.PENDING_REVIEW;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public RescheduleRequestStatusDTO toStatusDTO() {
        return new RescheduleRequestStatusDTO(requestId, bookingId, status, reason, preferredDates,
                requestedAt, reviewedAt);
    }
}
