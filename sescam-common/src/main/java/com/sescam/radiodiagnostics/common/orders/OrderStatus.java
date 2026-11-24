package com.sescam.radiodiagnostics.common.orders;

/**
 * Life cycle of an order. UC-PHY-01 only creates {@link #PENDING_SCHEDULING}; the other states are set by admissions
 * (SCHEDULED) and diagnostics (IN_PROGRESS, EXECUTED_PENDING_REPORT, INCOMPLETE_INCIDENT, REPORTED) in later use cases.
 */
public enum OrderStatus {
    PENDING_SCHEDULING,
    SCHEDULED,
    IN_PROGRESS,
    EXECUTED_PENDING_REPORT,
    INCOMPLETE_INCIDENT,
    REPORTED,
    CANCELLED
}
