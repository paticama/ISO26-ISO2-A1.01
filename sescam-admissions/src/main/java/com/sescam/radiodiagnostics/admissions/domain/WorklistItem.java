package com.sescam.radiodiagnostics.admissions.domain;

import com.sescam.radiodiagnostics.common.admissions.BookingStatus;
import com.sescam.radiodiagnostics.common.admissions.WorklistItemDTO;

import java.time.LocalDateTime;
import java.util.Objects;

/** Operational daily worklist entry synchronized with booking changes. */
public class WorklistItem {

    private final String worklistId;
    private final String bookingId;
    private final String orderId;
    private final String patientId;
    private final String resourceId;
    private final String modality;
    private final LocalDateTime scheduledStart;
    private final LocalDateTime scheduledEnd;
    private final BookingStatus status;

    public WorklistItem(String worklistId, String bookingId, String orderId, String patientId,
                        String resourceId, String modality, LocalDateTime scheduledStart,
                        LocalDateTime scheduledEnd, BookingStatus status) {
        this.worklistId = Objects.requireNonNull(worklistId);
        this.bookingId = Objects.requireNonNull(bookingId);
        this.orderId = Objects.requireNonNull(orderId);
        this.patientId = Objects.requireNonNull(patientId);
        this.resourceId = Objects.requireNonNull(resourceId);
        this.modality = Objects.requireNonNull(modality);
        this.scheduledStart = Objects.requireNonNull(scheduledStart);
        this.scheduledEnd = Objects.requireNonNull(scheduledEnd);
        this.status = Objects.requireNonNull(status);
    }

    public String getBookingId() {
        return bookingId;
    }

    public LocalDateTime getScheduledStart() {
        return scheduledStart;
    }

    public WorklistItemDTO toDTO() {
        return new WorklistItemDTO(worklistId, bookingId, orderId, patientId, resourceId, modality,
                scheduledStart, scheduledEnd, status);
    }
}
