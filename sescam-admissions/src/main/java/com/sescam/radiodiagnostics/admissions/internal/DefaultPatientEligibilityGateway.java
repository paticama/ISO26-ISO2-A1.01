package com.sescam.radiodiagnostics.admissions.internal;

import com.sescam.radiodiagnostics.common.admissions.PatientEligibilityGateway;

/** Stand-in patient eligibility gateway until wired to C6 (sescam-patients). */
public class DefaultPatientEligibilityGateway implements PatientEligibilityGateway {

    @Override
    public boolean isPatientEligible(String patientId) {
        return patientId != null && !patientId.isBlank();
    }
}
