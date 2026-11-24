package com.sescam.radiodiagnostics.client.patients;

import com.sescam.radiodiagnostics.common.patients.CreatePatientRequestDTO;
import com.sescam.radiodiagnostics.common.patients.DischargePatientRequestDTO;
import com.sescam.radiodiagnostics.common.patients.PatientDTO;
import com.sescam.radiodiagnostics.common.patients.PatientStatus;
import com.sescam.radiodiagnostics.common.patients.PatientSummaryDTO;
import com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO;

import java.util.List;

/**
 * Port interface for client-side communication with the patients REST API.
 */
public interface PatientApiClient {

    List<PatientSummaryDTO> search(String query, PatientStatus status, int page, int size,
                                  String actorId, String actorRole);

    PatientDTO getById(String id, String actorId, String actorRole);

    PatientDTO getByCip(String cip, String actorId, String actorRole);

    PatientDTO register(CreatePatientRequestDTO request);

    PatientDTO update(UpdatePatientRequestDTO request);

    PatientDTO discharge(DischargePatientRequestDTO request);
}
