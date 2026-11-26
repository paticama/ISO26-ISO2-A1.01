package com.sescam.radiodiagnostics.common.admissions;

/** Delivery state of the patient booking/rescheduling/cancellation notification (A4). */
public enum NotificationStatus {
    SENT,
    PENDING_RETRY,
    FAILED
}
