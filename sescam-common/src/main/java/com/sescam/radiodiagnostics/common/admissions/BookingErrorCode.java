package com.sescam.radiodiagnostics.common.admissions;

/** Explicit error codes defined by UC-AAD-05. */
public enum BookingErrorCode {
    INVALID_BOOKING_DATA,
    UNAUTHORIZED_BOOKING_ACCESS,
    BOOKING_NOT_FOUND,
    SLOT_UNAVAILABLE,
    REQUEST_ALREADY_PROCESSED,
    PATIENT_NOT_ELIGIBLE,
    BOOKING_CONFLICT,
    CONCURRENT_BOOKING
}
