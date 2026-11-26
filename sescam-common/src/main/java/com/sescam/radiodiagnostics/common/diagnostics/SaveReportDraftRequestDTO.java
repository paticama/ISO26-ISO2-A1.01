package com.sescam.radiodiagnostics.common.diagnostics;

/**
 * Request to save or update a diagnostic report draft without EHR sync or notification (5a of UC-PHY-04).
 */
public record SaveReportDraftRequestDTO(
        String executionId,
        String findings,
        String conclusion,
        boolean criticalAlert,
        boolean followUpRecommended,
        Long expectedVersion,
        String actorId,
        String actorRole
) {
}
