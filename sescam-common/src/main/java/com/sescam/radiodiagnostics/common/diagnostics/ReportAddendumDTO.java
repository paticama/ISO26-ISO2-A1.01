package com.sescam.radiodiagnostics.common.diagnostics;

import java.time.Instant;

/**
 * Post-signature amendment appended to a finalized diagnostic report (6a of UC-PHY-04).
 */
public record ReportAddendumDTO(
        String addendumId,
        String reason,
        String content,
        String authorPhysicianId,
        String digitalSignature,
        Instant createdAt
) {
}
