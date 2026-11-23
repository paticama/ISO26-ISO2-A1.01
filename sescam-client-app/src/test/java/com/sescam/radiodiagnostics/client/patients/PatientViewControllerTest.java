package com.sescam.radiodiagnostics.client.patients;

import com.sescam.radiodiagnostics.common.patients.AddressDTO;
import com.sescam.radiodiagnostics.common.patients.EmergencyContactDTO;
import com.sescam.radiodiagnostics.common.patients.Gender;
import com.sescam.radiodiagnostics.common.patients.PatientDTO;
import com.sescam.radiodiagnostics.common.patients.PatientStatus;
import com.sescam.radiodiagnostics.common.patients.PatientSummaryDTO;
import com.sescam.radiodiagnostics.common.patients.PreferredNotificationChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PatientViewController.class)
class PatientViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientApiClient apiClient;

    @Test
    @DisplayName("GET /patients renders list view with patient results")
    void listView() throws Exception {
        PatientSummaryDTO summary = new PatientSummaryDTO("p-1", "CIP100", "12345678Z", "Elena Blanco", LocalDate.of(1990, 1, 1), "600111222", PatientStatus.ACTIVE, 1L);
        when(apiClient.search(any(), any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(List.of(summary));

        mockMvc.perform(get("/patients"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/list"))
                .andExpect(content().string(containsString("Elena Blanco")))
                .andExpect(content().string(containsString("CIP100")));
    }

    @Test
    @DisplayName("GET /patients/new renders registration form")
    void showRegistrationForm() throws Exception {
        mockMvc.perform(get("/patients/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/register"))
                .andExpect(content().string(containsString("Patient Registration")));
    }

    @Test
    @DisplayName("POST /patients/new rejects invalid local fields and re-renders form")
    void registerLocalValidationFails() throws Exception {
        mockMvc.perform(post("/patients/new")
                        .param("cip", "SHORT")
                        .param("dni", "INVALID"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/register"))
                .andExpect(content().string(containsString("CIP is mandatory")));
    }

    @Test
    @DisplayName("POST /patients/new redirects to detail view on success")
    void registerSuccess() throws Exception {
        PatientDTO created = samplePatient("p-10", "CIP12345678");
        when(apiClient.register(any())).thenReturn(created);

        mockMvc.perform(post("/patients/new")
                        .param("cip", "CIP12345678")
                        .param("dni", "12345678Z")
                        .param("firstName", "Elena")
                        .param("lastName", "Blanco")
                        .param("birthDate", "1992-05-15")
                        .param("gender", "FEMALE")
                        .param("email", "elena@sescam.es"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patients/p-10"));

        verify(apiClient).register(any());
    }

    @Test
    @DisplayName("GET /patients/{id} renders detail view")
    void detailView() throws Exception {
        PatientDTO patient = samplePatient("p-10", "CIP12345678");
        when(apiClient.getById(eq("p-10"), any(), any())).thenReturn(patient);

        mockMvc.perform(get("/patients/p-10"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/detail"))
                .andExpect(content().string(containsString("Elena Blanco")))
                .andExpect(content().string(containsString("CIP12345678")));
    }

    @Test
    @DisplayName("POST /patients/{id}/update redirects to detail view on success")
    void updateSuccess() throws Exception {
        PatientDTO patient = samplePatient("p-10", "CIP12345678");
        when(apiClient.update(any())).thenReturn(patient);

        mockMvc.perform(post("/patients/p-10/update")
                        .param("firstName", "Elena Maria")
                        .param("lastName", "Blanco")
                        .param("expectedVersion", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patients/p-10"));

        verify(apiClient).update(any());
    }

    @Test
    @DisplayName("POST /patients/{id}/discharge redirects to detail view on success")
    void dischargeSuccess() throws Exception {
        PatientDTO patient = samplePatient("p-10", "CIP12345678");
        when(apiClient.discharge(any())).thenReturn(patient);

        mockMvc.perform(post("/patients/p-10/discharge")
                        .param("dischargeReason", "Relocated outside region")
                        .param("expectedVersion", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patients/p-10"));

        verify(apiClient).discharge(any());
    }

    @Test
    @DisplayName("GET /patients handles client error gracefully")
    void listError() throws Exception {
        when(apiClient.search(any(), any(), anyInt(), anyInt(), any(), any()))
                .thenThrow(new PatientClientException(PatientClientException.Kind.SERVER_UNREACHABLE, "Server unreachable", List.of(), null));

        mockMvc.perform(get("/patients"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/list"))
                .andExpect(content().string(containsString("Server unreachable")));
    }

    @Test
    @DisplayName("GET /patients/{id} handles not found error")
    void detailNotFound() throws Exception {
        when(apiClient.getById(eq("p-404"), any(), any()))
                .thenThrow(new PatientClientException(PatientClientException.Kind.NOT_FOUND, "Patient not found", List.of(), null));

        mockMvc.perform(get("/patients/p-404"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/detail"))
                .andExpect(content().string(containsString("Patient not found")));
    }

    @Test
    @DisplayName("POST /patients/new handles client conflict error")
    void registerConflict() throws Exception {
        when(apiClient.register(any()))
                .thenThrow(new PatientClientException(PatientClientException.Kind.CONFLICT, "CIP already registered", List.of("CIP duplicate"), null));

        mockMvc.perform(post("/patients/new")
                        .param("cip", "CIP12345678")
                        .param("dni", "12345678Z")
                        .param("firstName", "Elena")
                        .param("lastName", "Blanco")
                        .param("birthDate", "1992-05-15")
                        .param("gender", "FEMALE")
                        .param("email", "elena@sescam.es"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/register"))
                .andExpect(content().string(containsString("CIP already registered")));
    }

    @Test
    @DisplayName("POST /patients/{id}/update handles client error")
    void updateError() throws Exception {
        PatientDTO patient = samplePatient("p-10", "CIP12345678");
        when(apiClient.getById(eq("p-10"), any(), any())).thenReturn(patient);
        when(apiClient.update(any()))
                .thenThrow(new PatientClientException(PatientClientException.Kind.CONFLICT, "Optimistic lock failure", List.of(), null));

        mockMvc.perform(post("/patients/p-10/update")
                        .param("firstName", "Elena")
                        .param("lastName", "Blanco")
                        .param("expectedVersion", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/detail"))
                .andExpect(content().string(containsString("Optimistic lock failure")));
    }

    @Test
    @DisplayName("POST /patients/{id}/discharge handles client error")
    void dischargeError() throws Exception {
        PatientDTO patient = samplePatient("p-10", "CIP12345678");
        when(apiClient.getById(eq("p-10"), any(), any())).thenReturn(patient);
        when(apiClient.discharge(any()))
                .thenThrow(new PatientClientException(PatientClientException.Kind.CONFLICT, "Patient already discharged", List.of(), null));

        mockMvc.perform(post("/patients/p-10/discharge")
                        .param("dischargeReason", "Relocated")
                        .param("expectedVersion", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/detail"))
                .andExpect(content().string(containsString("Patient already discharged")));
    }

    private PatientDTO samplePatient(String id, String cip) {
        return new PatientDTO(
                id, cip, "12345678Z", "Elena", "Blanco", LocalDate.of(1992, 5, 15),
                Gender.FEMALE, "600111222", "elena@sescam.es",
                new AddressDTO("Calle Alarcos 1", "13001", "Ciudad Real", "Ciudad Real"),
                PreferredNotificationChannel.SMS,
                new EmergencyContactDTO("Pedro Blanco", "Father", "600000111"),
                "No allergies", PatientStatus.ACTIVE, 1L, Instant.now(), Instant.now(),
                "SYNCED", Instant.now()
        );
    }
}
