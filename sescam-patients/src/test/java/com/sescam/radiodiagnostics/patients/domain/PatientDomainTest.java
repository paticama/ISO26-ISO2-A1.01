package com.sescam.radiodiagnostics.patients.domain;

import com.sescam.radiodiagnostics.common.patients.Gender;
import com.sescam.radiodiagnostics.common.patients.PatientDTO;
import com.sescam.radiodiagnostics.common.patients.PatientStatus;
import com.sescam.radiodiagnostics.common.patients.PatientSummaryDTO;
import com.sescam.radiodiagnostics.common.patients.PreferredNotificationChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatientDomainTest {

    @Test
    @DisplayName("Patient entity initialization and default values")
    void initialization() {
        LocalDate birthDate = LocalDate.of(1985, 3, 20);
        Instant now = Instant.now();
        Patient patient = new Patient(
                "p-1", "CIP100", "01234567A", "Elena", "Blanco",
                birthDate, Gender.FEMALE, "611222333", "elena@example.com",
                new Address("Calle Toledo 5", "13002", "Ciudad Real", "Ciudad Real"),
                PreferredNotificationChannel.SMS,
                new EmergencyContact("Carlos Blanco", "Brother", "622333444"),
                "Hypertension", PatientStatus.ACTIVE, 1L, now, now, "PENDING_SYNC", null
        );

        assertThat(patient.getId()).isEqualTo("p-1");
        assertThat(patient.getCip()).isEqualTo("CIP100");
        assertThat(patient.getDni()).isEqualTo("01234567A");
        assertThat(patient.getFirstName()).isEqualTo("Elena");
        assertThat(patient.getLastName()).isEqualTo("Blanco");
        assertThat(patient.getBirthDate()).isEqualTo(birthDate);
        assertThat(patient.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(patient.getStatus()).isEqualTo(PatientStatus.ACTIVE);
        assertThat(patient.getVersion()).isEqualTo(1L);
        assertThat(patient.getEhrSyncStatus()).isEqualTo("PENDING_SYNC");

        PatientDTO dto = patient.toDTO();
        assertThat(dto.id()).isEqualTo("p-1");
        assertThat(dto.address().city()).isEqualTo("Ciudad Real");

        PatientSummaryDTO summary = patient.toSummaryDTO();
        assertThat(summary.fullName()).isEqualTo("Elena Blanco");
        assertThat(summary.cip()).isEqualTo("CIP100");
    }

    @Test
    @DisplayName("Mandatory fields enforce null checks")
    void mandatoryFields() {
        assertThatThrownBy(() -> new Patient(
                null, "CIP", "DNI", "First", "Last", LocalDate.now(),
                Gender.OTHER, "", "", null, null, null, null,
                PatientStatus.ACTIVE, 1L, null, null, null, null
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Update details increments version and updates timestamp")
    void updateDetails() {
        Patient patient = new Patient(
                "p-2", "CIP200", "11223344B", "Lucas", "Mora",
                LocalDate.of(1992, 7, 10), Gender.MALE, "600000001", "lucas@example.com",
                null, null, null, null, PatientStatus.ACTIVE, 1L,
                Instant.now(), Instant.now(), "SYNCED", Instant.now()
        );

        Instant updateTime = Instant.now().plusSeconds(60);
        patient.updateDetails("Lucas Antonio", "Mora Perez", "600000099", "lucas.mora@example.com",
                new Address("Calle Real 10", "13001", "Ciudad Real", "Ciudad Real"),
                PreferredNotificationChannel.EMAIL,
                new EmergencyContact("Ana Mora", "Sister", "600999888"),
                "Asthma", updateTime);

        assertThat(patient.getFirstName()).isEqualTo("Lucas Antonio");
        assertThat(patient.getLastName()).isEqualTo("Mora Perez");
        assertThat(patient.getPhone()).isEqualTo("600000099");
        assertThat(patient.getEmail()).isEqualTo("lucas.mora@example.com");
        assertThat(patient.getVersion()).isEqualTo(2L);
        assertThat(patient.getUpdatedAt()).isEqualTo(updateTime);
    }

    @Test
    @DisplayName("Discharge updates status to DISCHARGED and increments version")
    void discharge() {
        Patient patient = new Patient(
                "p-3", "CIP300", "22334455C", "Sara", "Cruz",
                LocalDate.of(2000, 1, 1), Gender.FEMALE, "600222333", "sara@example.com",
                null, null, null, null, PatientStatus.ACTIVE, 1L,
                Instant.now(), Instant.now(), "SYNCED", Instant.now()
        );

        Instant dischargeTime = Instant.now().plusSeconds(120);
        patient.discharge("Relocation outside regional healthcare district", dischargeTime);

        assertThat(patient.getStatus()).isEqualTo(PatientStatus.DISCHARGED);
        assertThat(patient.getDischargeReason()).isEqualTo("Relocation outside regional healthcare district");
        assertThat(patient.getVersion()).isEqualTo(2L);
        assertThat(patient.getUpdatedAt()).isEqualTo(dischargeTime);
    }
}
