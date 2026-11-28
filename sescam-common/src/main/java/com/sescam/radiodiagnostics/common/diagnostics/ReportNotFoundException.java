package com.sescam.radiodiagnostics.common.diagnostics;

/**
 * Requested diagnostic report or executed procedure was not found. HTTP 404.
 */
public class ReportNotFoundException extends RuntimeException {

    public ReportNotFoundException(String message) {
        super(message);
    }
}
