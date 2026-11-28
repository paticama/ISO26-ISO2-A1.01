package com.sescam.radiodiagnostics.common.diagnostics;

import java.time.Instant;

/**
 * Event emitted by C7 when a diagnostic report is finalized or amended so patient notifications can be sent (C8).
 */
public record ReportAvailabilityEvent(
        String reportId,
        String executionId,
        String orderId,
        String patientId,
        boolean criticalAlert,
        boolean addendum,
        Instant occurredAt
) {
}
