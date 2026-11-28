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
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Examination booking aggregate (UC-AAD-05); the patient confirms or cancels it in UC-PAT-01. */
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

    /**
     * The patient confirms they will attend (UC-PAT-01 step 8). Only a BOOKED or RESCHEDULED appointment can be
     * confirmed; the slot does not change. Policy checks (deadline, version) are done before calling this.
     */
    public void confirm(String actorId, Instant now) {
        if (this.status != BookingStatus.BOOKED && this.status != BookingStatus.RESCHEDULED) {
            throw new RequestAlreadyProcessedException(
                    "Booking '" + bookingId + "' cannot be confirmed in status " + status + ".");
        }
        changeStatus(BookingStatus.CONFIRMED, "Attendance confirmed by the patient", actorId, now);
    }

    /**
     * The examination was performed. Final state, like CANCELLED (UC-PAT-01 A7). To be called when C5 receives
     * the execution event published by C7 (UC-PHY-05).
     */
    public void markExecuted(String actorId, Instant now) {
        ensureActive();
        changeStatus(BookingStatus.EXECUTED, "Examination performed", actorId, now);
    }

    /** True if this appointment belongs to the given patient (UC-PAT-01 A8). */
    public boolean isOwnedBy(String candidatePatientId) {
        return candidatePatientId != null && patientId.equals(candidatePatientId.trim());
    }

    public void updateNotificationStatus(NotificationStatus notificationStatus) {
        this.notificationStatus = Objects.requireNonNull(notificationStatus);
    }

    /** Status change that keeps the same slot, recorded in the history with a new version. */
    private void changeStatus(BookingStatus newStatus, String reason, String actorId, Instant now) {
        BookingStatus previousStatus = this.status;
        this.status = newStatus;
        this.version++;
        this.history.add(new BookingHistoryEntry(UUID.randomUUID().toString(), previousStatus,
                newStatus, slotId, slotId, reason, actorId, now));
    }

    /** CANCELLED and EXECUTED are final: nothing can change the booking any more. */
    public boolean isFinal() {
        return status == BookingStatus.CANCELLED || status == BookingStatus.EXECUTED;
    }

    private void ensureActive() {
        if (isFinal()) {
            throw new RequestAlreadyProcessedException(
                    "Booking '" + bookingId + "' is already " + status.name().toLowerCase(Locale.ROOT) + ".");
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

    public String getModality() {
        return modality;
    }

    public String getSlotId() {
        return slotId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getLocationId() {
        return locationId;
    }

    public LocalDateTime getScheduledStart() {
        return scheduledStart;
    }

    public LocalDateTime getScheduledEnd() {
        return scheduledEnd;
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
