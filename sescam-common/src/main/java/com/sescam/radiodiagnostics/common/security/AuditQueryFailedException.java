package com.sescam.radiodiagnostics.common.security;

/**
 * The audit store could not be queried; the actor may retry (UC-ADM-03, A3).
 */
public class AuditQueryFailedException extends RuntimeException {

    public AuditQueryFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
