package com.sescam.radiodiagnostics.common.patients;

/**
 * Outbound port for synchronizing patient demographics with external EHR/HCE systems (ADR-004, ADR-014).
 */
public interface EhrGateway {

    EhrSyncResultDTO syncPatientRecord(PatientDTO patient);
}
