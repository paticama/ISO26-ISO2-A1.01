package com.sescam.radiodiagnostics.common.schedules;

/**
 * Life cycle of an examination schedule (UC-AAD-02).
 */
public enum ScheduleStatus {
    /** Slots can be booked by the booking workflow (UC-AAD-05). */
    OPEN,
    /** A3: no new bookings are accepted; existing appointments are preserved. */
    CLOSED
}
