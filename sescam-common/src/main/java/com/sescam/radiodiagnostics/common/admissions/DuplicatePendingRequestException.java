package com.sescam.radiodiagnostics.common.admissions;

/** Raised when an appointment already has a rescheduling request waiting for Admissions (UC-PAT-01 A6, HTTP 409). */
public class DuplicatePendingRequestException extends BookingException {

    public DuplicatePendingRequestException(String message) {
        super(BookingErrorCode.DUPLICATE_PENDING_REQUEST, message);
    }
}
