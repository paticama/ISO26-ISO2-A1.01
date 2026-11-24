package com.sescam.radiodiagnostics.patients.repository;

import com.sescam.radiodiagnostics.common.patients.Gender;
import com.sescam.radiodiagnostics.common.patients.PatientSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.patients.PatientStatus;
import com.sescam.radiodiagnostics.patients.domain.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryPatientRepositoryTest {

    private InMemoryPatientRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPatientRepository();
    }

    @Test
    @DisplayName("Save and findById returns persisted patient")
    void saveAndFindById() {
        Patient patient = createPatient("p-1", "CIP1000", "12345678Z", "Ana", "Ruiz");
        repository.save(patient);

        assertThat(repository.findById("p-1")).isPresent().contains(patient);
        assertThat(repository.findById("unknown")).isEmpty();
        assertThat(repository.findById(null)).isEmpty();
    }

    @Test
    @DisplayName("findByCip and findByDni are case-insensitive")
    void findByIdentifiers() {
        Patient patient = createPatient("p-2", "CIP2000", "87654321A", "David", "Sanz");
        repository.save(patient);

        assertThat(repository.findByCip("cip2000")).isPresent();
        assertThat(repository.findByCip("CIP2000")).isPresent();
        assertThat(repository.existsByCip("CIP2000")).isTrue();
        assertThat(repository.existsByCip("CIP9999")).isFalse();

        assertThat(repository.findByDni("87654321a")).isPresent();
        assertThat(repository.findByDni("87654321A")).isPresent();
        assertThat(repository.existsByDni("87654321A")).isTrue();
        assertThat(repository.existsByDni("00000000T")).isFalse();
    }

    @Test
    @DisplayName("Search filters by name, CIP, phone and status with pagination")
    void search() {
        Patient p1 = createPatient("p-1", "CIP1001", "11111111A", "Laura", "Gomez");
        Patient p2 = createPatient("p-2", "CIP1002", "22222222B", "Carlos", "Gomez");
        Patient p3 = createPatient("p-3", "CIP2003", "33333333C", "Marcos", "Lopez");
        p3.discharge("Moved", Instant.now());

        repository.save(p1);
        repository.save(p2);
        repository.save(p3);

        List<Patient> byLastName = repository.search(PatientSearchCriteriaDTO.ofQuery("Gomez"));
        assertThat(byLastName).hasSize(2);

        List<Patient> byCip = repository.search(PatientSearchCriteriaDTO.ofQuery("CIP2003"));
        assertThat(byCip).hasSize(1);

        List<Patient> activeOnly = repository.search(new PatientSearchCriteriaDTO("", PatientStatus.ACTIVE, 0, 10));
        assertThat(activeOnly).hasSize(2);

        List<Patient> dischargedOnly = repository.search(new PatientSearchCriteriaDTO("", PatientStatus.DISCHARGED, 0, 10));
        assertThat(dischargedOnly).hasSize(1);
    }

    private Patient createPatient(String id, String cip, String dni, String first, String last) {
        return new Patient(
                id, cip, dni, first, last, LocalDate.of(1990, 1, 1),
                Gender.UNKNOWN, "600123456", first.toLowerCase() + "@example.com",
                null, null, null, null, PatientStatus.ACTIVE, 1L,
                Instant.now(), Instant.now(), "PENDING_SYNC", null
        );
    }
}
