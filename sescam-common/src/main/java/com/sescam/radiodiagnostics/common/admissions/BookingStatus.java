package com.sescam.radiodiagnostics.common.admissions;

/** Lifecycle states of an examination booking (UC-AAD-05, UC-PAT-01). */
public enum BookingStatus {
    PENDING,
    BOOKED,
    RESCHEDULED,
    CANCELLED,
    /** The patient confirmed that they will attend (UC-PAT-01). BOOKED only means that staff booked it. */
    CONFIRMED,
    /** The examination was performed (set from the PHY-05 execution event). Final state, like CANCELLED. */
    EXECUTED
}
