package com.sescam.radiodiagnostics.common.diagnostics;

/**
 * The order is not a booked procedure. HTTP 404.
 */
public class ProcedureNotFoundException extends RuntimeException {
    public ProcedureNotFoundException(String message) {
        super(message);
    }
}
