package com.sescam.radiodiagnostics.common.diagnostics;

import java.time.Instant;

/**
 * Stored clinical incident (entity ClinicalIncident of C7), linked to the patient so it can be read in the patient's
 * clinical history (PAT-02) and in audits.
 */
public record ClinicalIncidentDTO(
        String incidentId,
        String executionId,
        String patientId,
        IncidentType type,
        IncidentSeverity severity,
        String description,
        String treatmentGiven,
        Instant occurredAt,
        boolean procedureAborted
) {
}
