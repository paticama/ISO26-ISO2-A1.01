package com.sescam.radiodiagnostics.diagnostics.internal;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.diagnostics.DicomStudyDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionRecordDTO;
import com.sescam.radiodiagnostics.common.diagnostics.PacsGateway;

import java.util.Locale;
import java.util.Objects;

/**
 * TEMPORARY stand-in for C9 (sescam-integration) RIS/PACS DICOM query adapter (C-FIND / WADO-RS).
 */
public class InMemoryPacsGateway implements PacsGateway {

    private static final String PACS_BASE_URL = "https://pacs.sescam.local/wado-rs/studies/";

    @Override
    public DicomStudyDTO fetchStudyForExecution(ExecutionRecordDTO execution) {
        Objects.requireNonNull(execution, "execution must not be null");
        String studyUid = studyUidFor(execution.executionId());
        int series = seriesCount(execution.modality());
        int instances = instanceCount(execution.modality());
        String token = "wado-" + execution.executionId().toLowerCase(Locale.ROOT);
        return new DicomStudyDTO(
                studyUid,
                execution.executionId(),
                execution.orderId(),
                execution.patientId(),
                "ACC-" + execution.orderId(),
                execution.modality(),
                execution.examTypeName(),
                series,
                instances,
                token,
                PACS_BASE_URL + studyUid,
                execution.endedAt()
        );
    }

    public static String studyUidFor(String executionId) {
        String normalized = executionId == null ? "UNKNOWN" : executionId.replaceAll("[^A-Za-z0-9]", "");
        return "1.2.826.0.1.3680043.8.498." + normalized;
    }

    private static int seriesCount(ModalityType modality) {
        if (modality == null) {
            return 1;
        }
        return switch (modality) {
            case CT, MRI -> 4;
            case NUCLEAR_MEDICINE -> 3;
            default -> 2;
        };
    }

    private static int instanceCount(ModalityType modality) {
        if (modality == null) {
            return 1;
        }
        return switch (modality) {
            case CT -> 180;
            case MRI -> 120;
            case NUCLEAR_MEDICINE -> 45;
            default -> 4;
        };
    }
}
