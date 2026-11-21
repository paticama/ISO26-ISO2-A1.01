package com.sescam.radiodiagnostics.common.security;

/**
 * The audit search is too broad: the time range or the number of matches exceeds the limit,
 * so the actor must refine it (UC-ADM-03, A3).
 */
public class AuditQueryTooBroadException extends RuntimeException {

    private final long limit;
    private final long actual;

    public AuditQueryTooBroadException(String message, long limit, long actual) {
        super(message);
        this.limit = limit;
        this.actual = actual;
    }

    public long getLimit() {
        return limit;
    }

    public long getActual() {
        return actual;
    }
}
