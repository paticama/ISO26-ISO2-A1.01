package com.sescam.radiodiagnostics.common.admissions;

/** Base exception for UC-AAD-05 booking operations. */
public abstract class BookingException extends RuntimeException {

    private final BookingErrorCode code;

    protected BookingException(BookingErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public BookingErrorCode getCode() {
        return code;
    }
}
