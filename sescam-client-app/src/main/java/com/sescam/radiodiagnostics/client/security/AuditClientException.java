package com.sescam.radiodiagnostics.client.security;

/**
 * Error returned by the server audit API, translated for the view (A1, A3).
 */
public class AuditClientException extends RuntimeException {

    public enum Kind { FORBIDDEN, INVALID_QUERY, QUERY_TOO_BROAD, QUERY_FAILED, SERVER_UNREACHABLE }

    private final Kind kind;

    public AuditClientException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public AuditClientException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    /** True when the actor can fix the problem by changing the filters (A3: refine). */
    public boolean canRefine() {
        return kind == Kind.INVALID_QUERY || kind == Kind.QUERY_TOO_BROAD;
    }

    /** True when the actor can simply try again (A3: retry). */
    public boolean canRetry() {
        return kind == Kind.QUERY_FAILED || kind == Kind.SERVER_UNREACHABLE;
    }
}
