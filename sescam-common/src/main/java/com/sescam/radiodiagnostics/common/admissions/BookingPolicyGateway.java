package com.sescam.radiodiagnostics.common.admissions;

/** Outbound port to C2 (sescam-catalogue) for booking and cancellation policies. */
public interface BookingPolicyGateway {

    int maxBookingsPerPatientPerDay();

    int minHoursBeforeCancellation();
}
