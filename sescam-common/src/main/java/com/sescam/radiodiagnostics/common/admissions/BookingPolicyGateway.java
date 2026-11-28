package com.sescam.radiodiagnostics.common.admissions;

/** Outbound port to C2 (sescam-catalogue) for booking and cancellation policies. */
public interface BookingPolicyGateway {

    int maxBookingsPerPatientPerDay();

    int minHoursBeforeCancellation();

    /**
     * Minimum hours before the appointment in which the patient may still confirm it (UC-PAT-01 A5).
     * 0 means no limit other than the appointment not having started yet.
     */
    default int minHoursBeforeConfirmation() {
        return 0;
    }

    /** How to contact the radiology service; shown to the patient when an action is not allowed (UC-PAT-01 A4, A5). */
    default String serviceContact() {
        return "Please contact the radiology service admissions desk.";
    }
}
