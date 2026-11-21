package com.sescam.radiodiagnostics.server.integration;

import com.sescam.radiodiagnostics.common.security.AuditQueryService;
import com.sescam.radiodiagnostics.common.security.AuditSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.security.AuditSearchResultDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC-ADM-04 over HTTP with the real wiring: C9 service, access control and the C1 audit trail.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class IntegrationRestControllerTest {

    private static final String SECRET = "S3cret-Value-123";
    private static final String BODY = "{\"endpointUrl\":\"https://sso.hospital.example/auth\",\"username\":\"svc\",\"secret\":\"" + SECRET + "\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditQueryService auditQueryService;

    @Test
    @DisplayName("GET /api/v1/integrations lists the four integrations for an administrator and gives 403 to others")
    void listEnforcesAuthorization() throws Exception {
        mockMvc.perform(get("/api/v1/integrations").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[3].id", is("sso")))
                .andExpect(jsonPath("$[3].status", is("NOT_CONFIGURED")));

        mockMvc.perform(get("/api/v1/integrations").header("X-Actor-Id", "patient-1").header("X-Actor-Role", "PATIENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
        mockMvc.perform(get("/api/v1/integrations/sso"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /{id} returns one integration; unknown id gives 404")
    void getOneAndNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/integrations/ehr").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("EHR")));
        mockMvc.perform(get("/api/v1/integrations/nope").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("Validate, save and review: the credential is never returned and the change is audited")
    void validateSaveAndReviewWithoutLeakingSecret() throws Exception {
        MvcResult validate = mockMvc.perform(post("/api/v1/integrations/sso/validate")
                        .header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.connection.status", is("NOT_CHECKED")))
                .andReturn();

        MvcResult update = mockMvc.perform(put("/api/v1/integrations/sso")
                        .header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"settings\":" + BODY + ",\"expectedVersion\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integrationId", is("sso")))
                .andExpect(jsonPath("$.newVersion", is(2)))
                .andExpect(jsonPath("$.updatedBy", is("admin-1")))
                .andReturn();

        MvcResult review = mockMvc.perform(get("/api/v1/integrations/sso")
                        .header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpointUrl", is("https://sso.hospital.example/auth")))
                .andExpect(jsonPath("$.credentialConfigured", is(true)))
                .andExpect(jsonPath("$.version", is(2)))
                .andReturn();

        for (MvcResult result : new MvcResult[]{validate, update, review}) {
            assertThat(result.getResponse().getContentAsString()).doesNotContain(SECRET);
        }

        AuditSearchResultDTO audit = auditQueryService.searchEvents(new AuditSearchCriteriaDTO(
                Instant.now().minusSeconds(3600), Instant.now().plusSeconds(3600), "admin-1",
                "INTEGRATION_CONFIGURATION_UPDATED", "sso"), "admin-1", "ADMINISTRATOR");
        assertThat(audit.totalMatches()).isEqualTo(1);
        assertThat(audit.events().get(0).newValue()).isEqualTo("https://sso.hospital.example/auth");
        assertThat(audit.events().get(0).toString()).doesNotContain(SECRET);
    }

    @Test
    @DisplayName("PUT with invalid settings gives 400 with the list of problems and keeps the configuration; no secret echoed")
    void invalidSettingsGive400() throws Exception {
        MvcResult result = mockMvc.perform(put("/api/v1/integrations/sso")
                        .header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"settings\":{\"endpointUrl\":\"http://plain.example\",\"secret\":\"short\"},\"expectedVersion\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_SETTINGS")))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("short\"");

        mockMvc.perform(get("/api/v1/integrations/sso").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(jsonPath("$.status", is("NOT_CONFIGURED")))
                .andExpect(jsonPath("$.version", is(1)));
    }

    @Test
    @DisplayName("POST /validate with invalid settings gives 200 with valid=false")
    void validateInvalidSettings() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/sso/validate")
                        .header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"endpointUrl\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)));
    }

    @Test
    @DisplayName("PUT with an old version gives 409; a malformed body gives 400 INVALID_REQUEST")
    void conflictAndMalformedBody() throws Exception {
        String update = "{\"settings\":" + BODY + ",\"expectedVersion\":%d}";
        mockMvc.perform(put("/api/v1/integrations/sso").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                .contentType(MediaType.APPLICATION_JSON).content(update.formatted(1))).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/integrations/sso").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                        .contentType(MediaType.APPLICATION_JSON).content(update.formatted(1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("CONCURRENT_MODIFICATION")))
                .andExpect(jsonPath("$.actualVersion", is(2)));

        MvcResult malformed = mockMvc.perform(put("/api/v1/integrations/sso")
                        .header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"settings\": {\"secret\": \"" + SECRET + "\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_REQUEST")))
                .andReturn();
        assertThat(malformed.getResponse().getContentAsString()).doesNotContain(SECRET);
    }

    @Test
    @DisplayName("POST /{id}/check reports NOT_CONFIGURED before and NOT_CHECKED after configuring; unauthorized gets 403")
    void checkConnectionEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/sso/check").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("NOT_CONFIGURED")));

        mockMvc.perform(put("/api/v1/integrations/sso").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                .contentType(MediaType.APPLICATION_JSON).content("{\"settings\":" + BODY + ",\"expectedVersion\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/integrations/sso/check").header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("NOT_CHECKED")));

        mockMvc.perform(post("/api/v1/integrations/sso/check").header("X-Actor-Id", "patient-1").header("X-Actor-Role", "PATIENT"))
                .andExpect(status().isForbidden());
    }
}
