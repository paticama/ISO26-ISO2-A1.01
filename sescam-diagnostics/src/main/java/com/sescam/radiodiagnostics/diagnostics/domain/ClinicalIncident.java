package com.sescam.radiodiagnostics.diagnostics.domain;

import com.sescam.radiodiagnostics.common.diagnostics.ClinicalIncidentDTO;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentSeverity;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentType;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity of C7: a clinical or operational incident during an examination (2a/2b of UC-PHY-05). It carries the patient
 * id so it can be read in the patient's clinical history and in audits.
 */
public class ClinicalIncident {

    private final String id;
    private final String executionId;
    private final String patientId;
    private final IncidentType type;
    private final IncidentSeverity severity;
    private final String description;
    private final String treatmentGiven;
    private final Instant occurredAt;
    private final boolean procedureAborted;

    public ClinicalIncident(String id, String executionId, String patientId, IncidentType type,
                            IncidentSeverity severity, String description, String treatmentGiven,
                            Instant occurredAt, boolean procedureAborted) {
        this.id = Objects.requireNonNull(id, "id");
        this.executionId = Objects.requireNonNull(executionId, "executionId");
        this.patientId = Objects.requireNonNull(patientId, "patientId");
        this.type = Objects.requireNonNull(type, "type");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.description = Objects.requireNonNull(description, "description");
        this.treatmentGiven = treatmentGiven;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.procedureAborted = procedureAborted;
    }

    public ClinicalIncidentDTO toDTO() {
        return new ClinicalIncidentDTO(id, executionId, patientId, type, severity, description, treatmentGiven,
                occurredAt, procedureAborted);
    }

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public IncidentType getType() {
        return type;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }
}
