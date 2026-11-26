package com.sescam.radiodiagnostics.common.diagnostics;

/**
 * Outcome of exporting a finalized diagnostic report to the Hospital EHR via HL7 FHIR.
 */
public enum EhrReportSyncStatus {
    /** Report is still in DRAFT; EHR synchronization has not run. */
    NOT_SYNCED,
    /** HL7 FHIR DiagnosticReport resource was synchronized successfully. */
    SYNCED,
    /** External EHR FHIR export failed; visible to the physician for retry. */
    FAILED_SYNC
}
