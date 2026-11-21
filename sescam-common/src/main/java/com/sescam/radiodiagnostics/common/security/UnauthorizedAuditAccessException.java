package com.sescam.radiodiagnostics.common.security;

/**
 * The requester is not allowed to view audit records (UC-ADM-03, A1).
 */
public class UnauthorizedAuditAccessException extends RuntimeException {

    public UnauthorizedAuditAccessException(String message) {
        super(message);
    }
}
