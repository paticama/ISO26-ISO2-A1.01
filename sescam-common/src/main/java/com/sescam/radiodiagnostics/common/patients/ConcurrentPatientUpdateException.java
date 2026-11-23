package com.sescam.radiodiagnostics.common.patients;

public class ConcurrentPatientUpdateException extends RuntimeException {
    public ConcurrentPatientUpdateException(String message) {
        super(message);
    }
}
