package com.sescam.radiodiagnostics.common.admissions;

import java.time.LocalDateTime;
import java.util.List;

/** Confirmed, rescheduled, or cancelled examination booking (UC-AAD-05). */
public record BookingDTO(
        String bookingId,
        String orderId,
        String patientId,
        String modality,
        String slotId,
        String resourceId,
        String locationId,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd,
        BookingStatus status,
        NotificationStatus notificationStatus,
        long version,
        List<BookingHistoryEntryDTO> history
) {
}
