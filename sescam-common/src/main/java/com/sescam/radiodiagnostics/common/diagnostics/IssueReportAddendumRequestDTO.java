package com.sescam.radiodiagnostics.common.diagnostics;

/**
 * Request to append a post-signature addendum to a finalized diagnostic report (6a of UC-PHY-04).
 */
public record IssueReportAddendumRequestDTO(
        String reportId,
        String reason,
        String addendumContent,
        String digitalSignature,
        Long expectedVersion,
        String actorId,
        String actorRole
) {
}
