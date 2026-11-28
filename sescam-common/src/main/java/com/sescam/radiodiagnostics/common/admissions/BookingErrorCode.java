package com.sescam.radiodiagnostics.common.admissions;

/** Explicit error codes defined by UC-AAD-05 and UC-PAT-01. */
public enum BookingErrorCode {
    INVALID_BOOKING_DATA,
    UNAUTHORIZED_BOOKING_ACCESS,
    BOOKING_NOT_FOUND,
    SLOT_UNAVAILABLE,
    REQUEST_ALREADY_PROCESSED,
    PATIENT_NOT_ELIGIBLE,
    BOOKING_CONFLICT,
    CONCURRENT_BOOKING,
    /** UC-PAT-01 A6: the appointment already has a rescheduling request waiting for Admissions. */
    DUPLICATE_PENDING_REQUEST
}
