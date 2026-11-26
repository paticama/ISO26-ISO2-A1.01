package com.sescam.radiodiagnostics.common.admissions;

import java.util.List;

/** Raised when mandatory booking fields are missing or invalid (HTTP 400). */
public class InvalidBookingDataException extends BookingException {

    private final List<String> invalidFields;

    public InvalidBookingDataException(String message, List<String> invalidFields) {
        super(BookingErrorCode.INVALID_BOOKING_DATA, message);
        this.invalidFields = invalidFields == null ? List.of() : List.copyOf(invalidFields);
    }

    public List<String> getInvalidFields() {
        return invalidFields;
    }
}
