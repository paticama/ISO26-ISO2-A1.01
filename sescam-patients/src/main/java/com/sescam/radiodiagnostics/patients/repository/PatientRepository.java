package com.sescam.radiodiagnostics.patients.repository;

import com.sescam.radiodiagnostics.common.patients.PatientSearchCriteriaDTO;
import com.sescam.radiodiagnostics.patients.domain.Patient;

import java.util.List;
import java.util.Optional;

public interface PatientRepository {

    Patient save(Patient patient);

    Optional<Patient> findById(String id);

    Optional<Patient> findByCip(String cip);

    Optional<Patient> findByDni(String dni);

    List<Patient> search(PatientSearchCriteriaDTO criteria);

    boolean existsByCip(String cip);

    boolean existsByDni(String dni);

    long count();
}
