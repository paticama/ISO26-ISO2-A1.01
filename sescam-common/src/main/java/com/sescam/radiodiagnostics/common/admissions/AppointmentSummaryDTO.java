package com.sescam.radiodiagnostics.common.admissions;

import java.time.LocalDateTime;

/**
 * One appointment in the patient's list (UC-PAT-01 steps 2-3).
 *
 * @param examTypeName name of the examination, taken from the order (C4); null if the order is not available
 * @param resourceId   room or equipment of the slot (C3)
 */
public record AppointmentSummaryDTO(
        String bookingId,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd,
        String examTypeName,
        String modality,
        String locationId,
        String resourceId,
        BookingStatus status
) {
}
