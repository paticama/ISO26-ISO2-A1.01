package com.sescam.radiodiagnostics.admissions.domain;

import com.sescam.radiodiagnostics.common.admissions.BookingDTO;
import com.sescam.radiodiagnostics.common.admissions.BookingStatus;
import com.sescam.radiodiagnostics.common.admissions.NotificationStatus;
import com.sescam.radiodiagnostics.common.admissions.RequestAlreadyProcessedException;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Examination booking aggregate (UC-AAD-05). */
public class Booking {

    private final String bookingId;
    private final String orderId;
    private final String patientId;
    private final String modality;
    private String slotId;
    private String resourceId;
    private String locationId;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private BookingStatus status;
    private NotificationStatus notificationStatus;
    private long version;
    private final List<BookingHistoryEntry> history = new ArrayList<>();

    public Booking(String bookingId, String orderId, String patientId, String modality,
                   SlotDTO slot, String actorId, Instant createdAt) {
        this.bookingId = Objects.requireNonNull(bookingId);
        this.orderId = Objects.requireNonNull(orderId);
        this.patientId = Objects.requireNonNull(patientId);
        this.modality = Objects.requireNonNull(modality);
        applySlot(Objects.requireNonNull(slot));
        this.status = BookingStatus.BOOKED;
        this.notificationStatus = NotificationStatus.PENDING_RETRY;
        this.version = 1L;
        this.history.add(new BookingHistoryEntry(UUID.randomUUID().toString(), BookingStatus.PENDING,
                BookingStatus.BOOKED, null, slot.slotId(), "Initial booking", actorId, createdAt));
    }

    public void reschedule(SlotDTO newSlot, String reason, String actorId, Instant now) {
        ensureActive();
        BookingStatus previousStatus = this.status;
        String previousSlotId = this.slotId;
        applySlot(Objects.requireNonNull(newSlot));
        this.status = BookingStatus.RESCHEDULED;
        this.version++;
        this.history.add(new BookingHistoryEntry(UUID.randomUUID().toString(), previousStatus,
                BookingStatus.RESCHEDULED, previousSlotId, newSlot.slotId(), reason, actorId, now));
    }

    public void cancel(String reason, String actorId, Instant now) {
        ensureActive();
        BookingStatus previousStatus = this.status;
        String previousSlotId = this.slotId;
        this.status = BookingStatus.CANCELLED;
        this.version++;
        this.history.add(new BookingHistoryEntry(UUID.randomUUID().toString(), previousStatus,
                BookingStatus.CANCELLED, previousSlotId, null, reason, actorId, now));
    }

    public void updateNotificationStatus(NotificationStatus notificationStatus) {
        this.notificationStatus = Objects.requireNonNull(notificationStatus);
    }

    private void ensureActive() {
        if (this.status == BookingStatus.CANCELLED) {
            throw new RequestAlreadyProcessedException("Booking '" + bookingId + "' is already cancelled.");
        }
    }

    private void applySlot(SlotDTO slot) {
        this.slotId = slot.slotId();
        this.resourceId = slot.resourceId();
        this.locationId = slot.locationId();
        this.scheduledStart = slot.start();
        this.scheduledEnd = slot.end();
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getSlotId() {
        return slotId;
    }

    public LocalDateTime getScheduledStart() {
        return scheduledStart;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public BookingDTO toDTO() {
        return new BookingDTO(
                bookingId, orderId, patientId, modality, slotId, resourceId, locationId,
                scheduledStart, scheduledEnd, status, notificationStatus, version,
                history.stream().map(BookingHistoryEntry::toDTO).toList()
        );
    }
}
