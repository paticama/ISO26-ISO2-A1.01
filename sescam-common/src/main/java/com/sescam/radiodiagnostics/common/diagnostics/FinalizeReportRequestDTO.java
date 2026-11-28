package com.sescam.radiodiagnostics.common.diagnostics;

/**
 * Request to validate, digitally sign, and finalize a diagnostic report (Step 6 of UC-PHY-04).
 */
public record FinalizeReportRequestDTO(
        String executionId,
        String findings,
        String conclusion,
        boolean criticalAlert,
        boolean followUpRecommended,
        String digitalSignature,
        Long expectedVersion,
        String actorId,
        String actorRole
) {
}
