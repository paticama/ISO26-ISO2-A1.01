package com.sescam.radiodiagnostics.client.security;

/**
 * Error of the UC-ADM-02 server API that is not a role-change result, translated for the view (A1, A3).
 */
public class RoleAdminClientException extends RuntimeException {

    public enum Kind { FORBIDDEN, USER_NOT_FOUND, INVALID_REQUEST, FAILED, SERVER_UNREACHABLE }

    private final Kind kind;

    public RoleAdminClientException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public RoleAdminClientException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    /** True when the administrator can simply try again. */
    public boolean canRetry() {
        return kind == Kind.FAILED || kind == Kind.SERVER_UNREACHABLE;
    }
}
