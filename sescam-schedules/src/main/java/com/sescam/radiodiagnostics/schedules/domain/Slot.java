package com.sescam.radiodiagnostics.schedules.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One time slot of a {@link Schedule}. Holds the booking references (C5) that took a place in it, in the
 * order they were reserved.
 */
public class Slot {

    private final String id;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final int capacity;
    private final List<String> bookingReferences;
    private long version;

    Slot(String id, LocalDateTime start, LocalDateTime end, int capacity, List<String> bookingReferences,
         long version) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.capacity = capacity;
        this.bookingReferences = new ArrayList<>(bookingReferences);
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public int getCapacity() {
        return capacity;
    }

    public long getVersion() {
        return version;
    }

    public List<String> getBookingReferences() {
        return Collections.unmodifiableList(bookingReferences);
    }

    public int getBookedCount() {
        return bookingReferences.size();
    }

    public boolean hasFreeCapacity() {
        return bookingReferences.size() < capacity;
    }

    public boolean holds(String bookingReference) {
        return bookingReferences.contains(bookingReference);
    }

    void addBooking(String bookingReference) {
        bookingReferences.add(bookingReference);
        version++;
    }

    boolean removeBooking(String bookingReference) {
        boolean removed = bookingReferences.remove(bookingReference);
        if (removed) {
            version++;
        }
        return removed;
    }
}
