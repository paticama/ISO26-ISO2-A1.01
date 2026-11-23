package com.sescam.radiodiagnostics.common.patients;

public class UnauthorizedPatientAccessException extends RuntimeException {
    public UnauthorizedPatientAccessException(String message) {
        super(message);
    }
}
