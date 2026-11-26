package com.sescam.radiodiagnostics.common.diagnostics;

/**
 * Outbound port to C9 (sescam-integration) / RIS-PACS for querying DICOM study metadata and WADO tokens (UC-PHY-04).
 */
public interface PacsGateway {

    /**
     * Queries the PACS archive via C-FIND / WADO-RS for an executed procedure.
     *
     * @throws PacsConnectionException if the PACS archive is unreachable or returns an error (2a)
     */
    DicomStudyDTO fetchStudyForExecution(ExecutionRecordDTO execution);
}
