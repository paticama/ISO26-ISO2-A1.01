package com.sescam.radiodiagnostics.common.diagnostics;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;

import java.time.Instant;
import java.util.List;

/**
 * Stored execution returned to the client (step 5 of UC-PHY-05).
 *
 * @param executionId execution record id
 * @param status      EXECUTED_PENDING_REPORT, or INCOMPLETE_INCIDENT when aborted (2b)
 * @param incidents   stored incidents, each linked to the patient
 * @param recordedBy  actor that logged the execution
 */
public record ExecutionRecordDTO(
        String executionId,
        String orderId,
        String patientId,
        String examTypeName,
        ModalityType modality,
        Instant startedAt,
        Instant endedAt,
        String roomEquipmentId,
        String contrastBatch,
        Double contrastVolumeMl,
        Double doseDlp,
        Double doseCtdiVol,
        Double radiotracerActivityMBq,
        boolean informedConsentVerified,
        ExecutionStatus status,
        String abortReason,
        List<ClinicalIncidentDTO> incidents,
        String recordedBy,
        Instant recordedAt
) {
    public ExecutionRecordDTO {
        incidents = incidents == null ? List.of() : List.copyOf(incidents);
    }
}
