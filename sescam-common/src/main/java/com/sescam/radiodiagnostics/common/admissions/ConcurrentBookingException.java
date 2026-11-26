package com.sescam.radiodiagnostics.common.admissions;

/** Raised when stale slot or booking version is detected during concurrent booking (A5, HTTP 409). */
public class ConcurrentBookingException extends BookingException {

    public ConcurrentBookingException(String message) {
        super(BookingErrorCode.CONCURRENT_BOOKING, message);
    }
}
