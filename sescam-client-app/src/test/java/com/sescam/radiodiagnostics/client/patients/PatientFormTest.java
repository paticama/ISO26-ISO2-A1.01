package com.sescam.radiodiagnostics.client.patients;

import com.sescam.radiodiagnostics.common.patients.AddressDTO;
import com.sescam.radiodiagnostics.common.patients.CreatePatientRequestDTO;
import com.sescam.radiodiagnostics.common.patients.DischargePatientRequestDTO;
import com.sescam.radiodiagnostics.common.patients.EmergencyContactDTO;
import com.sescam.radiodiagnostics.common.patients.Gender;
import com.sescam.radiodiagnostics.common.patients.PatientDTO;
import com.sescam.radiodiagnostics.common.patients.PatientStatus;
import com.sescam.radiodiagnostics.common.patients.PreferredNotificationChannel;
import com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PatientFormTest {

    @Test
    @DisplayName("Validation fails when mandatory registration fields are missing or invalid")
    void validationFailures() {
        PatientForm form = new PatientForm();
        form.setCip("123"); // too short
        form.setDni("INVALID");
        form.setBirthDate("2050-01-01"); // future
        form.setEmail("invalid-email");

        Map<String, String> errors = form.validateRegistration();
        assertThat(errors).containsKey("cip")
                .containsKey("dni")
                .containsKey("firstName")
                .containsKey("lastName")
                .containsKey("birthDate")
                .containsKey("email");
    }

    @Test
    @DisplayName("Validation passes for correctly filled registration form")
    void validRegistration() {
        PatientForm form = new PatientForm();
        form.setCip("CIP12345678");
        form.setDni("12345678Z");
        form.setFirstName("Laura");
        form.setLastName("Gomez");
        form.setBirthDate("1990-05-10");
        form.setEmail("laura@sescam.es");

        Map<String, String> errors = form.validateRegistration();
        assertThat(errors).isEmpty();

        CreatePatientRequestDTO req = form.toCreateRequest("actor-1", "ROLE");
        assertThat(req.cip()).isEqualTo("CIP12345678");
        assertThat(req.birthDate()).isEqualTo(LocalDate.of(1990, 5, 10));
    }

    @Test
    @DisplayName("Update validation flags blank names and invalid email")
    void updateValidation() {
        PatientForm form = new PatientForm();
        form.setFirstName(" ");
        form.setEmail("bad");

        Map<String, String> errors = form.validateUpdate();
        assertThat(errors).containsKey("firstName").containsKey("email");
    }

    @Test
    @DisplayName("fromDTO correctly maps all fields into form")
    void fromDto() {
        LocalDate birthDate = LocalDate.of(1982, 11, 3);
        Instant now = Instant.now();
        PatientDTO dto = new PatientDTO(
                "p-99", "CIP9999", "99998888K", "Tomas", "Diaz", birthDate,
                Gender.MALE, "600111222", "tomas@test.es",
                new AddressDTO("Calle Real 1", "13001", "Ciudad Real", "Ciudad Real"),
                PreferredNotificationChannel.EMAIL,
                new EmergencyContactDTO("Sonia Diaz", "Sister", "600999000"),
                "No notes", PatientStatus.ACTIVE, 3L, now, now, "SYNCED", now
        );

        PatientForm form = PatientForm.fromDTO(dto);
        assertThat(form.getId()).isEqualTo("p-99");
        assertThat(form.getCip()).isEqualTo("CIP9999");
        assertThat(form.getFirstName()).isEqualTo("Tomas");
        assertThat(form.getStreet()).isEqualTo("Calle Real 1");
        assertThat(form.getExpectedVersion()).isEqualTo(3L);

        UpdatePatientRequestDTO updateReq = form.toUpdateRequest("u-1", "ROLE");
        assertThat(updateReq.expectedVersion()).isEqualTo(3L);

        form.setDischargeReason("Patient moved");
        DischargePatientRequestDTO dischargeReq = form.toDischargeRequest("u-1", "ROLE");
        assertThat(dischargeReq.dischargeReason()).isEqualTo("Patient moved");
    }
}
