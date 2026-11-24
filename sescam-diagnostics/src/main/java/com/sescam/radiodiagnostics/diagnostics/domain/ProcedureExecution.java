package com.sescam.radiodiagnostics.diagnostics.domain;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionRecordDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Entity of C7: the execution record of a booked procedure (UC-PHY-05), with its incidents.
 * One record per order; it is not edited after it is stored.
 */
public class ProcedureExecution {

    private final String id;
    private final String orderId;
    private final String patientId;
    private final String examTypeName;
    private final ModalityType modality;
    private final Instant startedAt;
    private final Instant endedAt;
    private final String roomEquipmentId;
    private final String contrastBatch;
    private final Double contrastVolumeMl;
    private final Double doseDlp;
    private final Double doseCtdiVol;
    private final Double radiotracerActivityMBq;
    private final boolean informedConsentVerified;
    private final ExecutionStatus status;
    private final String abortReason;
    private final List<ClinicalIncident> incidents;
    private final String recordedBy;
    private final Instant recordedAt;

    public ProcedureExecution(String id, String orderId, String patientId, String examTypeName, ModalityType modality,
                              Instant startedAt, Instant endedAt, String roomEquipmentId,
                              String contrastBatch, Double contrastVolumeMl,
                              Double doseDlp, Double doseCtdiVol, Double radiotracerActivityMBq,
                              boolean informedConsentVerified, ExecutionStatus status, String abortReason,
                              List<ClinicalIncident> incidents, String recordedBy, Instant recordedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.patientId = Objects.requireNonNull(patientId, "patientId");
        this.examTypeName = examTypeName;
        this.modality = modality;
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.endedAt = Objects.requireNonNull(endedAt, "endedAt");
        this.roomEquipmentId = Objects.requireNonNull(roomEquipmentId, "roomEquipmentId");
        this.contrastBatch = contrastBatch;
        this.contrastVolumeMl = contrastVolumeMl;
        this.doseDlp = doseDlp;
        this.doseCtdiVol = doseCtdiVol;
        this.radiotracerActivityMBq = radiotracerActivityMBq;
        this.informedConsentVerified = informedConsentVerified;
        this.status = Objects.requireNonNull(status, "status");
        this.abortReason = abortReason;
        this.incidents = List.copyOf(incidents == null ? List.of() : incidents);
        this.recordedBy = Objects.requireNonNull(recordedBy, "recordedBy");
        this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt");
    }

    public ExecutionRecordDTO toDTO() {
        return new ExecutionRecordDTO(id, orderId, patientId, examTypeName, modality, startedAt, endedAt,
                roomEquipmentId, contrastBatch, contrastVolumeMl, doseDlp, doseCtdiVol, radiotracerActivityMBq,
                informedConsentVerified, status, abortReason,
                incidents.stream().map(ClinicalIncident::toDTO).toList(), recordedBy, recordedAt);
    }

    public String getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getPatientId() {
        return patientId;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public String getAbortReason() {
        return abortReason;
    }

    public List<ClinicalIncident> getIncidents() {
        return incidents;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
