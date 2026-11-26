package com.sescam.radiodiagnostics.common.diagnostics;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;

import java.time.Instant;

/**
 * Executed diagnostic procedure shown in the physician's reporting worklist (Step 1 of UC-PHY-04).
 */
public record ReportableProcedureDTO(
        String executionId,
        String orderId,
        String patientId,
        String examTypeName,
        ModalityType modality,
        Instant executedAt,
        String reportId,
        ReportStatus reportStatus
) {
}
