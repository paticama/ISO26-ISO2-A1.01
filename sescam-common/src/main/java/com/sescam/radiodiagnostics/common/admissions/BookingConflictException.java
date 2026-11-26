package com.sescam.radiodiagnostics.common.admissions;

/** Raised when a booking violates a patient-day or cancellation policy conflict (HTTP 409). */
public class BookingConflictException extends BookingException {

    public BookingConflictException(String message) {
        super(BookingErrorCode.BOOKING_CONFLICT, message);
    }
}
