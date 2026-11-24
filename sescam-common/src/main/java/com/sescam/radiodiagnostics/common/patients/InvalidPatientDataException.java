package com.sescam.radiodiagnostics.common.patients;

public class InvalidPatientDataException extends RuntimeException {
    public InvalidPatientDataException(String message) {
        super(message);
    }
}
