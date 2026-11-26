package com.sescam.radiodiagnostics.common.admissions;

/** Raised when an order has already been booked or a booking is already cancelled (HTTP 409). */
public class RequestAlreadyProcessedException extends BookingException {

    public RequestAlreadyProcessedException(String message) {
        super(BookingErrorCode.REQUEST_ALREADY_PROCESSED, message);
    }
}
