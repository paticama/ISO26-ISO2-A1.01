package com.sescam.radiodiagnostics.common.diagnostics;

import java.time.Instant;

/**
 * A clinical or operational incident documented during the examination (2a of UC-PHY-05).
 *
 * @param type           mandatory
 * @param severity       mandatory
 * @param description    clinical narrative; mandatory
 * @param treatmentGiven treatment or action taken; may be {@code null}
 * @param occurredAt     when it happened; {@code null} means the end of the execution
 */
public record IncidentDTO(
        IncidentType type,
        IncidentSeverity severity,
        String description,
        String treatmentGiven,
        Instant occurredAt
) {
}
