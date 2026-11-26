package com.sescam.radiodiagnostics.common.admissions;

import java.time.LocalDateTime;

/** Operational daily worklist item updated on booking, rescheduling, and cancellation. */
public record WorklistItemDTO(
        String worklistId,
        String bookingId,
        String orderId,
        String patientId,
        String resourceId,
        String modality,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd,
        BookingStatus status
) {
}
