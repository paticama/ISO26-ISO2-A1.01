package com.sescam.radiodiagnostics.common.diagnostics;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;

import java.time.Instant;
import java.util.List;

/**
 * Full diagnostic report representation shared between server and client (UC-PHY-04).
 */
public record ReportDTO(
        String reportId,
        String executionId,
        String orderId,
        String patientId,
        String studyInstanceUid,
        String examTypeName,
        ModalityType modality,
        String findings,
        String conclusion,
        boolean criticalAlert,
        boolean followUpRecommended,
        ReportStatus status,
        String authorPhysicianId,
        String digitalSignature,
        Instant signedAt,
        EhrReportSyncStatus ehrSyncStatus,
        Instant ehrSyncedAt,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<ReportAddendumDTO> addendums
) {
    public ReportDTO {
        addendums = addendums == null ? List.of() : List.copyOf(addendums);
    }
}
