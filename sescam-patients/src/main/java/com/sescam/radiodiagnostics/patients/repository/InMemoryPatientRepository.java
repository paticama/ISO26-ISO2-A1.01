package com.sescam.radiodiagnostics.patients.repository;

import com.sescam.radiodiagnostics.common.patients.PatientSearchCriteriaDTO;
import com.sescam.radiodiagnostics.patients.domain.Patient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPatientRepository implements PatientRepository {

    private final Map<String, Patient> storage = new ConcurrentHashMap<>();

    @Override
    public Patient save(Patient patient) {
        storage.put(patient.getId(), patient);
        return patient;
    }

    @Override
    public Optional<Patient> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<Patient> findByCip(String cip) {
        if (cip == null) return Optional.empty();
        String c = cip.trim().toUpperCase(Locale.ROOT);
        return storage.values().stream()
                .filter(p -> p.getCip().toUpperCase(Locale.ROOT).equals(c))
                .findFirst();
    }

    @Override
    public Optional<Patient> findByDni(String dni) {
        if (dni == null) return Optional.empty();
        String d = dni.trim().toUpperCase(Locale.ROOT);
        return storage.values().stream()
                .filter(p -> p.getDni().toUpperCase(Locale.ROOT).equals(d))
                .findFirst();
    }

    @Override
    public List<Patient> search(PatientSearchCriteriaDTO criteria) {
        String q = criteria.query() == null ? "" : criteria.query().trim().toLowerCase(Locale.ROOT);
        return storage.values().stream()
                .filter(p -> {
                    if (criteria.status() != null && p.getStatus() != criteria.status()) {
                        return false;
                    }
                    if (q.isBlank()) {
                        return true;
                    }
                    return p.getCip().toLowerCase(Locale.ROOT).contains(q)
                            || p.getDni().toLowerCase(Locale.ROOT).contains(q)
                            || p.getFirstName().toLowerCase(Locale.ROOT).contains(q)
                            || p.getLastName().toLowerCase(Locale.ROOT).contains(q)
                            || p.getPhone().contains(q);
                })
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .skip((long) criteria.page() * criteria.size())
                .limit(criteria.size())
                .toList();
    }

    @Override
    public boolean existsByCip(String cip) {
        return findByCip(cip).isPresent();
    }

    @Override
    public boolean existsByDni(String dni) {
        return findByDni(dni).isPresent();
    }

    @Override
    public long count() {
        return storage.size();
    }
}
