package com.sescam.radiodiagnostics.common.admissions;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * A patient's rescheduling request as the patient sees it (UC-PAT-01 A2, A6).
 *
 * @param reviewedAt when Admissions accepted or rejected it; null while PENDING_REVIEW
 */
public record RescheduleRequestStatusDTO(
        String requestId,
        String bookingId,
        RescheduleRequestStatus status,
        String reason,
        List<LocalDate> preferredDates,
        Instant requestedAt,
        Instant reviewedAt
) {
    public RescheduleRequestStatusDTO {
        preferredDates = preferredDates == null ? List.of() : List.copyOf(preferredDates);
    }
}
