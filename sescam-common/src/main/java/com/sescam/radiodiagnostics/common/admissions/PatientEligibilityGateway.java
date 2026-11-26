package com.sescam.radiodiagnostics.common.admissions;

/** Outbound port to C6 (sescam-patients) to verify patient eligibility before booking. */
public interface PatientEligibilityGateway {

    boolean isPatientEligible(String patientId);
}
