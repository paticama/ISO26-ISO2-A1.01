package com.sescam.radiodiagnostics.common.admissions;

/** Raised when an actor is not authorized to perform booking operations (A6, HTTP 403). */
public class UnauthorizedBookingAccessException extends BookingException {

    public UnauthorizedBookingAccessException(String message) {
        super(BookingErrorCode.UNAUTHORIZED_BOOKING_ACCESS, message);
    }
}
