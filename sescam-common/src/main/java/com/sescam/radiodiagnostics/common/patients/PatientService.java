package com.sescam.radiodiagnostics.common.patients;

import java.util.List;

/**
 * Public service contract for C6 (sescam-patients) realizing UC-AAD-01: Manage Patient.
 */
public interface PatientService {

    /**
     * Registers a new patient record with mandatory fields, duplicate detection (A1), and audit logging.
     */
    PatientDTO registerPatient(CreatePatientRequestDTO request);

    /**
     * Consults a patient record by unique internal ID.
     */
    PatientDTO getPatientById(String patientId, String actorId, String actorRole);

    /**
     * Consults a patient record by regional healthcare identifier (CIP).
     */
    PatientDTO getPatientByCip(String cip, String actorId, String actorRole);

    /**
     * Searches patient records by query (matching CIP, DNI, name, or phone) and optional status filter.
     */
    List<PatientSummaryDTO> searchPatients(PatientSearchCriteriaDTO criteria, String actorId, String actorRole);

    /**
     * Updates an existing patient record with optimistic locking verification (A4) and audit logging.
     */
    PatientDTO updatePatient(UpdatePatientRequestDTO request);

    /**
     * Discharges or deactivates a patient record with reason and audit logging.
     */
    PatientDTO dischargePatient(DischargePatientRequestDTO request);
}
