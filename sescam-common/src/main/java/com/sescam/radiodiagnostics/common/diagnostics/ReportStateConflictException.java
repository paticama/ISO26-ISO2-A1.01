package com.sescam.radiodiagnostics.common.diagnostics;

/**
 * Report lifecycle or optimistic-locking conflict (e.g., editing a finalized report directly, adding addendum to a draft,
 * or stale version). HTTP 409.
 */
public class ReportStateConflictException extends RuntimeException {

    public ReportStateConflictException(String message) {
        super(message);
    }
}
