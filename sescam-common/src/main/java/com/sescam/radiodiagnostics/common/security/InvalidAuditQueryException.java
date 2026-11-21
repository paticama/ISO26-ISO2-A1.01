package com.sescam.radiodiagnostics.common.security;

/**
 * The audit search criteria are missing or inconsistent, e.g. {@code from} after {@code to} (UC-ADM-03, A3).
 */
public class InvalidAuditQueryException extends RuntimeException {

    public InvalidAuditQueryException(String message) {
        super(message);
    }
}
