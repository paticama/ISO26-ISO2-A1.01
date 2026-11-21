package com.sescam.radiodiagnostics.client.integration;

import com.sescam.radiodiagnostics.common.integration.ConnectionCheckResultDTO;
import com.sescam.radiodiagnostics.common.integration.IntegrationSettingsDTO;
import com.sescam.radiodiagnostics.common.integration.IntegrationStatus;
import com.sescam.radiodiagnostics.common.integration.IntegrationSummaryDTO;
import com.sescam.radiodiagnostics.common.integration.IntegrationType;
import com.sescam.radiodiagnostics.common.integration.SettingsCheckResultDTO;
import com.sescam.radiodiagnostics.common.integration.UpdateIntegrationResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(IntegrationViewController.class)
class IntegrationViewControllerTest {

    private static final String SECRET = "S3cret-Value-123";
    private static final Instant NOW = Instant.parse("2026-11-21T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IntegrationApiClient apiClient;

    private static IntegrationSummaryDTO sso(boolean configured, IntegrationStatus status, long version) {
        return new IntegrationSummaryDTO("sso", IntegrationType.SSO, "Single sign-on (SSO)",
                configured ? "https://sso.hospital.example/auth" : null, configured ? "svc" : null, configured,
                status, "status text", version, NOW, "admin-1", NOW);
    }

    @Test
    @DisplayName("Steps 1-2: the list shows each integration with its status; the credential is not shown")
    void listShowsIntegrationsAndStatus() throws Exception {
        when(apiClient.list("admin-1", "ADMINISTRATOR")).thenReturn(List.of(
                sso(true, IntegrationStatus.UNAVAILABLE, 2L),
                new IntegrationSummaryDTO("ehr", IntegrationType.EHR, "Electronic health record (EHR/HCE)", null, null,
                        false, IntegrationStatus.NOT_CONFIGURED, "Not configured yet.", 1L, null, "system", NOW)));

        mockMvc.perform(get("/integrations").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk())
                .andExpect(view().name("integrations/list"))
                .andExpect(content().string(containsString("Single sign-on (SSO)")))
                .andExpect(content().string(containsString("UNAVAILABLE")))
                .andExpect(content().string(containsString("NOT_CONFIGURED")))
                .andExpect(content().string(containsString("Stored")))
                .andExpect(content().string(containsString("https://sso.hospital.example/auth")));
    }

    @Test
    @DisplayName("A1: an unauthorized user sees the server's refusal and no integrations")
    void listForbidden() throws Exception {
        when(apiClient.list("patient-1", "PATIENT")).thenThrow(
                new IntegrationClientException(IntegrationClientException.Kind.FORBIDDEN, "You are not authorized."));

        mockMvc.perform(get("/integrations").header("X-Actor-Id", "patient-1").header("X-Actor-Role", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You are not authorized.")))
                .andExpect(content().string(not(containsString("Single sign-on"))));
    }

    @Test
    @DisplayName("A3: when the server cannot be reached the list offers a retry")
    void listServerUnreachable() throws Exception {
        when(apiClient.list(any(), any())).thenThrow(
                new IntegrationClientException(IntegrationClientException.Kind.SERVER_UNREACHABLE, "The server cannot be reached."));

        mockMvc.perform(get("/integrations"))
                .andExpect(content().string(containsString("The server cannot be reached.")))
                .andExpect(content().string(containsString("Retry")));
    }

    @Test
    @DisplayName("Step 3: the form is prefilled without the credential and says whether one is stored")
    void editPrefillsFormWithoutCredential() throws Exception {
        when(apiClient.get("sso", "admin-1", "ADMINISTRATOR")).thenReturn(sso(true, IntegrationStatus.CONNECTED, 2L));

        mockMvc.perform(get("/integrations/sso").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk())
                .andExpect(view().name("integrations/edit"))
                .andExpect(content().string(containsString("value=\"https://sso.hospital.example/auth\"")))
                .andExpect(content().string(containsString("name=\"expectedVersion\" value=\"2\"")))
                .andExpect(content().string(containsString("Stored (hidden)")))
                .andExpect(content().string(containsString("type=\"password\"")))
                .andExpect(content().string(containsString("Retry connection check")));
    }

    @Test
    @DisplayName("Unknown integration shows the error and no form")
    void editNotFound() throws Exception {
        when(apiClient.get(eq("nope"), any(), any())).thenThrow(
                new IntegrationClientException(IntegrationClientException.Kind.NOT_FOUND, "The integration does not exist."));

        mockMvc.perform(get("/integrations/nope"))
                .andExpect(content().string(containsString("The integration does not exist.")))
                .andExpect(content().string(not(containsString("integration-form"))));
    }

    @Test
    @DisplayName("Step 5: local validation errors are shown and the server is not called")
    void validateRejectsLocally() throws Exception {
        when(apiClient.get("sso", "admin-1", "ADMINISTRATOR")).thenReturn(sso(false, IntegrationStatus.NOT_CONFIGURED, 1L));

        mockMvc.perform(post("/integrations/sso/validate").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                        .param("endpointUrl", "http://plain.example").param("secret", "short").param("expectedVersion", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Endpoint URL must use https.")))
                .andExpect(content().string(containsString("between 8 and 256")))
                .andExpect(content().string(not(containsString("short\""))));

        verify(apiClient, never()).validate(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Step 5: a failed connection check is shown as a failure and Save is not recommended; no credential in the page")
    void validateShowsFailedCheck() throws Exception {
        when(apiClient.get("sso", "admin-1", "ADMINISTRATOR")).thenReturn(sso(false, IntegrationStatus.NOT_CONFIGURED, 1L));
        when(apiClient.validate(eq("sso"), any(IntegrationSettingsDTO.class), eq("admin-1"), eq("ADMINISTRATOR")))
                .thenReturn(SettingsCheckResultDTO.checked("sso", new ConnectionCheckResultDTO("sso",
                        IntegrationStatus.UNAVAILABLE, "No answer from the system.", NOW)));

        mockMvc.perform(post("/integrations/sso/validate").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                        .param("endpointUrl", "https://sso.hospital.example/auth").param("secret", SECRET)
                        .param("expectedVersion", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("UNAVAILABLE")))
                .andExpect(content().string(containsString("No answer from the system.")))
                .andExpect(content().string(containsString("would not replace the working configuration")))
                .andExpect(content().string(not(containsString(SECRET))));
    }

    @Test
    @DisplayName("Step 5: a successful check invites the administrator to confirm")
    void validateShowsSuccessfulCheck() throws Exception {
        when(apiClient.get("sso", "admin-1", "ADMINISTRATOR")).thenReturn(sso(true, IntegrationStatus.CONNECTED, 2L));
        when(apiClient.validate(eq("sso"), any(), any(), any())).thenReturn(SettingsCheckResultDTO.checked("sso",
                new ConnectionCheckResultDTO("sso", IntegrationStatus.CONNECTED, "Connected.", NOW)));

        mockMvc.perform(post("/integrations/sso/validate").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                        .param("endpointUrl", "https://sso.hospital.example/auth").param("expectedVersion", "2"))
                .andExpect(content().string(containsString("press Save to confirm")));
    }

    @Test
    @DisplayName("Step 5: server-side validation problems are listed")
    void validateShowsServerErrors() throws Exception {
        when(apiClient.get("sso", "admin-1", "ADMINISTRATOR")).thenReturn(sso(true, IntegrationStatus.CONNECTED, 2L));
        when(apiClient.validate(eq("sso"), any(), any(), any())).thenReturn(
                SettingsCheckResultDTO.invalid("sso", List.of("Endpoint URL is not a valid URL.")));

        mockMvc.perform(post("/integrations/sso/validate").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                        .param("endpointUrl", "https://sso.hospital.example/auth").param("expectedVersion", "2"))
                .andExpect(content().string(containsString("The settings are not valid.")))
                .andExpect(content().string(containsString("Endpoint URL is not a valid URL.")));
    }

    @Test
    @DisplayName("Steps 6-7: a saved change is confirmed with version and status, uses the typed settings and never echoes the credential")
    void saveConfirmsTheChange() throws Exception {
        when(apiClient.get("sso", "admin-1", "ADMINISTRATOR"))
                .thenReturn(sso(false, IntegrationStatus.NOT_CONFIGURED, 1L))
                .thenReturn(sso(true, IntegrationStatus.NOT_CHECKED, 2L));
        when(apiClient.update(eq("sso"), any(IntegrationSettingsDTO.class), anyLong(), eq("admin-1"), eq("ADMINISTRATOR")))
                .thenReturn(new UpdateIntegrationResponseDTO("sso", IntegrationStatus.NOT_CHECKED, "Not tested.", 2L, "admin-1", NOW));

        mockMvc.perform(post("/integrations/sso/save").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                        .param("endpointUrl", " https://sso.hospital.example/auth ").param("username", "svc")
                        .param("secret", SECRET).param("expectedVersion", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Settings saved.")))
                .andExpect(content().string(containsString("Version 2, status NOT_CHECKED")))
                .andExpect(content().string(containsString("name=\"expectedVersion\" value=\"2\"")))
                .andExpect(content().string(not(containsString(SECRET))));

        verify(apiClient).update("sso", new IntegrationSettingsDTO("https://sso.hospital.example/auth", "svc", SECRET),
                1L, "admin-1", "ADMINISTRATOR");
    }

    @Test
    @DisplayName("Local validation also blocks Save")
    void saveRejectsLocally() throws Exception {
        when(apiClient.get("sso", "admin-1", "ADMINISTRATOR")).thenReturn(sso(false, IntegrationStatus.NOT_CONFIGURED, 1L));

        mockMvc.perform(post("/integrations/sso/save").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                        .param("endpointUrl", "").param("expectedVersion", "1"))
                .andExpect(content().string(containsString("Endpoint URL is required.")));

        verify(apiClient, never()).update(any(), any(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("A2: when the server refuses the change the explanation is shown and the previous configuration is kept")
    void saveFailsBecauseOfFailedCheck() throws Exception {
        when(apiClient.get("sso", "admin-1", "ADMINISTRATOR")).thenReturn(sso(true, IntegrationStatus.CONNECTED, 2L));
        when(apiClient.update(eq("sso"), any(), anyLong(), any(), any())).thenThrow(new IntegrationClientException(
                IntegrationClientException.Kind.CONNECTION_FAILED, "The system rejected the credentials.", List.of(),
                "CONNECTION_FAILED", null));

        mockMvc.perform(post("/integrations/sso/save").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                        .param("endpointUrl", "https://other.example").param("secret", SECRET).param("expectedVersion", "2"))
                .andExpect(content().string(containsString("The system rejected the credentials.")))
                .andExpect(content().string(containsString("previous working configuration was kept")))
                .andExpect(content().string(containsString("Correct the settings")))
                .andExpect(content().string(not(containsString(SECRET))));
    }

    @Test
    @DisplayName("A3: when the system is unavailable the page offers a retry")
    void saveFailsBecauseSystemUnavailable() throws Exception {
        when(apiClient.get("sso", "admin-1", "ADMINISTRATOR")).thenReturn(sso(true, IntegrationStatus.CONNECTED, 2L));
        when(apiClient.update(eq("sso"), any(), anyLong(), any(), any())).thenThrow(new IntegrationClientException(
                IntegrationClientException.Kind.CONNECTION_FAILED, "No answer.", List.of(), "UNAVAILABLE", null));

        mockMvc.perform(post("/integrations/sso/save").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                        .param("endpointUrl", "https://sso.hospital.example/auth").param("expectedVersion", "2"))
                .andExpect(content().string(containsString("You can retry.")));
    }

    @Test
    @DisplayName("Server-side invalid settings are listed one by one")
    void saveShowsServerValidationErrors() throws Exception {
        when(apiClient.get("sso", "admin-1", "ADMINISTRATOR")).thenReturn(sso(true, IntegrationStatus.CONNECTED, 2L));
        when(apiClient.update(eq("sso"), any(), anyLong(), any(), any())).thenThrow(new IntegrationClientException(
                IntegrationClientException.Kind.INVALID_SETTINGS, "Invalid.", List.of("Problem one.", "Problem two."), null, null));

        mockMvc.perform(post("/integrations/sso/save").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                        .param("endpointUrl", "https://sso.hospital.example/auth").param("expectedVersion", "2"))
                .andExpect(content().string(containsString("Problem one.")))
                .andExpect(content().string(containsString("Problem two.")));
    }

    @Test
    @DisplayName("A3: the retry shows the new status of the saved configuration")
    void checkShowsResult() throws Exception {
        when(apiClient.get("sso", "admin-1", "ADMINISTRATOR")).thenReturn(sso(true, IntegrationStatus.UNAVAILABLE, 2L));
        when(apiClient.check("sso", "admin-1", "ADMINISTRATOR")).thenReturn(
                new ConnectionCheckResultDTO("sso", IntegrationStatus.UNAVAILABLE, "No answer.", NOW));

        mockMvc.perform(post("/integrations/sso/check").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                        .param("expectedVersion", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("check-result")))
                .andExpect(content().string(containsString("No answer.")));
    }

    @Test
    @DisplayName("A1/A3: a refused retry shows the error")
    void checkFailure() throws Exception {
        when(apiClient.get(any(), any(), any())).thenThrow(
                new IntegrationClientException(IntegrationClientException.Kind.FORBIDDEN, "Denied."));
        when(apiClient.check(any(), any(), any())).thenThrow(
                new IntegrationClientException(IntegrationClientException.Kind.FORBIDDEN, "Denied."));

        mockMvc.perform(post("/integrations/sso/check"))
                .andExpect(content().string(containsString("Denied.")));
    }
}
