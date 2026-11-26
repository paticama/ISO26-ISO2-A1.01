package com.sescam.radiodiagnostics.admissions.domain;

import com.sescam.radiodiagnostics.common.admissions.BookingHistoryEntryDTO;
import com.sescam.radiodiagnostics.common.admissions.BookingStatus;

import java.time.Instant;
import java.util.Objects;

/** Immutable domain entry recording a booking state or slot transition. */
public class BookingHistoryEntry {

    private final String entryId;
    private final BookingStatus previousStatus;
    private final BookingStatus newStatus;
    private final String previousSlotId;
    private final String newSlotId;
    private final String reason;
    private final String changedBy;
    private final Instant changedAt;

    public BookingHistoryEntry(String entryId, BookingStatus previousStatus, BookingStatus newStatus,
                               String previousSlotId, String newSlotId, String reason,
                               String changedBy, Instant changedAt) {
        this.entryId = Objects.requireNonNull(entryId);
        this.previousStatus = previousStatus;
        this.newStatus = Objects.requireNonNull(newStatus);
        this.previousSlotId = previousSlotId;
        this.newSlotId = newSlotId;
        this.reason = reason;
        this.changedBy = Objects.requireNonNull(changedBy);
        this.changedAt = Objects.requireNonNull(changedAt);
    }

    public BookingHistoryEntryDTO toDTO() {
        return new BookingHistoryEntryDTO(entryId, previousStatus, newStatus, previousSlotId, newSlotId,
                reason, changedBy, changedAt);
    }
}
