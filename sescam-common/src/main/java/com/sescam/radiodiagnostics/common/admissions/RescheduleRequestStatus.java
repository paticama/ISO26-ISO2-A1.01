package com.sescam.radiodiagnostics.common.admissions;

/** States of a rescheduling request raised by a patient (UC-PAT-01 A2) and processed by Admissions (UC-AAD-05). */
public enum RescheduleRequestStatus {
    PENDING_REVIEW,
    ACCEPTED,
    REJECTED
}
