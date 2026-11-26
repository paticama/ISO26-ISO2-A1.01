package com.sescam.radiodiagnostics.admissions.internal;

import com.sescam.radiodiagnostics.common.admissions.BookingPolicyGateway;

/** Default booking and cancellation policy until wired to C2 (sescam-catalogue). */
public class DefaultBookingPolicyGateway implements BookingPolicyGateway {

    @Override
    public int maxBookingsPerPatientPerDay() {
        return 3;
    }

    @Override
    public int minHoursBeforeCancellation() {
        return 0;
    }
}
