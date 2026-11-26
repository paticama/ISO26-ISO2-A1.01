package com.sescam.radiodiagnostics.common.diagnostics;

/**
 * Lifecycle state of a diagnostic report (UC-PHY-04).
 */
public enum ReportStatus {
    /** 5a: saved as draft; no EHR sync or patient notification is triggered. */
    DRAFT,
    /** Steps 6-7: validated and digitally signed; synchronized with EHR and patient notified. */
    FINALIZED,
    /** 6a: finalized report with one or more post-signature addendums preserving the original signature. */
    AMENDED
}
