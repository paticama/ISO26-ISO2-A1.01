package com.sescam.radiodiagnostics.common.patients;

import com.sescam.radiodiagnostics.common.security.AccessControlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PatientContractsTest {

    @Test
    @DisplayName("AddressDTO and EmergencyContactDTO sanitize nulls to empty strings")
    void recordSanitization() {
        AddressDTO address = new AddressDTO(null, null, null, null);
        assertThat(address.street()).isEmpty();
        assertThat(address.postalCode()).isEmpty();
        assertThat(address.city()).isEmpty();
        assertThat(address.province()).isEmpty();

        EmergencyContactDTO contact = new EmergencyContactDTO(null, null, null);
        assertThat(contact.name()).isEmpty();
        assertThat(contact.relationship()).isEmpty();
        assertThat(contact.phone()).isEmpty();
    }

    @Test
    @DisplayName("PatientSearchCriteriaDTO sanitizes negative pagination and null query")
    void searchCriteriaSanitization() {
        PatientSearchCriteriaDTO criteria = new PatientSearchCriteriaDTO(null, PatientStatus.ACTIVE, -5, 0);
        assertThat(criteria.query()).isEmpty();
        assertThat(criteria.status()).isEqualTo(PatientStatus.ACTIVE);
        assertThat(criteria.page()).isZero();
        assertThat(criteria.size()).isEqualTo(20);

        PatientSearchCriteriaDTO simple = PatientSearchCriteriaDTO.ofQuery("Garcia");
        assertThat(simple.query()).isEqualTo("Garcia");
        assertThat(simple.page()).isZero();
        assertThat(simple.size()).isEqualTo(50);
    }

    @Test
    @DisplayName("EhrSyncResultDTO factory methods create appropriate responses")
    void ehrSyncResultFactory() {
        EhrSyncResultDTO ok = EhrSyncResultDTO.success("EXT-1234");
        assertThat(ok.synced()).isTrue();
        assertThat(ok.externalReference()).isEqualTo("EXT-1234");
        assertThat(ok.message()).contains("Synchronized");
        assertThat(ok.syncTimestamp()).isNotNull();

        EhrSyncResultDTO fail = EhrSyncResultDTO.failure("EHR unreachable");
        assertThat(fail.synced()).isFalse();
        assertThat(fail.externalReference()).isNull();
        assertThat(fail.message()).isEqualTo("EHR unreachable");
    }

    @Test
    @DisplayName("Domain exceptions preserve error details")
    void exceptions() {
        assertThat(new PatientNotFoundException("Patient not found").getMessage()).isEqualTo("Patient not found");
        assertThat(new DuplicatePatientException("CIP already exists").getMessage()).isEqualTo("CIP already exists");
        assertThat(new InvalidPatientDataException("Invalid DNI").getMessage()).isEqualTo("Invalid DNI");
        assertThat(new ConcurrentPatientUpdateException("Version conflict").getMessage()).isEqualTo("Version conflict");
        assertThat(new UnauthorizedPatientAccessException("Denied").getMessage()).isEqualTo("Denied");
        assertThat(new EhrSyncException("Sync failed").getMessage()).isEqualTo("Sync failed");
    }

    @Test
    @DisplayName("AccessControlService default canManagePatients allows administrative and admissions roles")
    void accessControlDefault() {
        AccessControlService ac = (actorId, role) -> false;

        assertThat(ac.canManagePatients("u1", "ADMINISTRATIVE_STAFF")).isTrue();
        assertThat(ac.canManagePatients("u2", "ADMISSIONS_STAFF")).isTrue();
        assertThat(ac.canManagePatients("u3", "RECEPTIONIST")).isTrue();
        assertThat(ac.canManagePatients("u4", "ADMINISTRATOR")).isTrue();
        assertThat(ac.canManagePatients("u5", "PHYSICIAN")).isFalse();
        assertThat(ac.canManagePatients("u6", "PATIENT")).isFalse();
        assertThat(ac.canManagePatients("u7", null)).isFalse();
        assertThat(ac.canManagePatients("u8", "   ")).isFalse();
    }

    @Test
    @DisplayName("PatientDTO correctly stores demographic and contact records")
    void patientDtoFull() {
        LocalDate birthDate = LocalDate.of(1990, 5, 15);
        Instant now = Instant.now();
        AddressDTO address = new AddressDTO("Calle Mayor 1", "13001", "Ciudad Real", "Ciudad Real");
        EmergencyContactDTO contact = new EmergencyContactDTO("Maria Garcia", "Spouse", "600111222");

        PatientDTO patient = new PatientDTO(
                "p-1", "CIP1234567890", "12345678Z", "Juan", "Garcia",
                birthDate, Gender.MALE, "600000000", "juan@example.com",
                address, PreferredNotificationChannel.SMS, contact,
                "No known drug allergies", PatientStatus.ACTIVE,
                1L, now, now, "SYNCED", now
        );

        assertThat(patient.id()).isEqualTo("p-1");
        assertThat(patient.cip()).isEqualTo("CIP1234567890");
        assertThat(patient.dni()).isEqualTo("12345678Z");
        assertThat(patient.gender()).isEqualTo(Gender.MALE);
        assertThat(patient.status()).isEqualTo(PatientStatus.ACTIVE);
        assertThat(patient.version()).isEqualTo(1L);
        assertThat(patient.address().city()).isEqualTo("Ciudad Real");
        assertThat(patient.emergencyContact().relationship()).isEqualTo("Spouse");
    }
}
