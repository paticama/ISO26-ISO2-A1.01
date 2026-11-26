package com.sescam.radiodiagnostics.common.diagnostics;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;

import java.time.Instant;

/**
 * DICOM study metadata and WADO-RS access token returned by the departmental RIS/PACS (FR-PHY-12).
 */
public record DicomStudyDTO(
        String studyInstanceUid,
        String executionId,
        String orderId,
        String patientId,
        String accessionNumber,
        ModalityType modality,
        String examTypeName,
        int numberOfSeries,
        int numberOfInstances,
        String wadoViewerToken,
        String pacsEndpointUrl,
        Instant acquiredAt
) {
}
