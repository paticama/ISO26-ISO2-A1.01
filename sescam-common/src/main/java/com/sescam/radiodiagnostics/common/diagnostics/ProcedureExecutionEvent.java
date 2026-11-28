package com.sescam.radiodiagnostics.common.diagnostics;

import java.time.Instant;

/**
 * Event published by C7 when an execution is recorded.
 *
 * @param status      EXECUTED_PENDING_REPORT, or INCOMPLETE_INCIDENT when the procedure was aborted
 * @param abortReason reason of the abort; {@code null} unless INCOMPLETE_INCIDENT
 */
public record ProcedureExecutionEvent(
        String executionId,
        String orderId,
        String patientId,
        ExecutionStatus status,
        String abortReason,
        Instant occurredAt
) {
    /** True when admissions must reschedule the procedure (2b). */
    public boolean requiresRescheduling() {
        return status == ExecutionStatus.INCOMPLETE_INCIDENT;
    }
}
