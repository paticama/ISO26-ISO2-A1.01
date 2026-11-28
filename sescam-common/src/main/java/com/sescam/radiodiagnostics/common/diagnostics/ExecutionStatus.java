package com.sescam.radiodiagnostics.common.diagnostics;

/**
 * Result of logging an execution (UC-PHY-05). Matches the order states of C4 with the same names.
 */
public enum ExecutionStatus {
    /** Step 4: executed, waiting for the report (UC-PHY-04). */
    EXECUTED_PENDING_REPORT,
    /** 2b: aborted by an incident; admissions reschedules. */
    INCOMPLETE_INCIDENT
}
