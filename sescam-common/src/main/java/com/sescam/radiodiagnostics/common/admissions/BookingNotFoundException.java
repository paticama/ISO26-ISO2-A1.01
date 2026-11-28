package com.sescam.radiodiagnostics.common.admissions;

/** Raised when a booking or order cannot be found (HTTP 404). */
public class BookingNotFoundException extends BookingException {

    public BookingNotFoundException(String message) {
        super(BookingErrorCode.BOOKING_NOT_FOUND, message);
    }
}
