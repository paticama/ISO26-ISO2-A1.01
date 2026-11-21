package com.sescam.radiodiagnostics.server.catalogue;

import com.sescam.radiodiagnostics.common.catalogue.ConfigurationSaveException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ConfigurationRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/configurations returns parameters for authorized admin and 403 for unauthorized user")
    void listAndGetParametersEnforceAuthorization() throws Exception {
        mockMvc.perform(get("/api/v1/configurations")
                        .header("X-Actor-Id", "admin-1")
                        .header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(5))));

        mockMvc.perform(get("/api/v1/configurations/triage.urgent.sla.hours")
                        .header("X-Actor-Id", "admin-1")
                        .header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key", is("triage.urgent.sla.hours")));

        mockMvc.perform(get("/api/v1/configurations")
                        .header("X-Actor-Id", "patient-1")
                        .header("X-Actor-Role", "PATIENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("POST /validate and PUT /configurations handle valid update, invalid value (400), not found (404), and conflict (409)")
    void validateUpdateAndErrorFlowsOverHttp() throws Exception {
        // Validate invalid value
        mockMvc.perform(post("/api/v1/configurations/booking.cancellation.min.hours/validate")
                        .header("X-Actor-Id", "admin-1")
                        .header("X-Actor-Role", "ADMINISTRATOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proposedValue\":\"999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)));

        // Valid update (version 1 -> 2)
        mockMvc.perform(put("/api/v1/configurations/booking.cancellation.min.hours")
                        .header("X-Actor-Id", "admin-1")
                        .header("X-Actor-Role", "ADMINISTRATOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proposedValue\":\"36\",\"expectedVersion\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentValue", is("36")))
                .andExpect(jsonPath("$.newVersion", is(2)))
                .andExpect(jsonPath("$.activationStatus", is("ACTIVE")));

        // Stale version -> 409 Conflict (Alt A4)
        mockMvc.perform(put("/api/v1/configurations/booking.cancellation.min.hours")
                        .header("X-Actor-Id", "admin-2")
                        .header("X-Actor-Role", "ADMINISTRATOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proposedValue\":\"12\",\"expectedVersion\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("CONCURRENT_MODIFICATION")))
                .andExpect(jsonPath("$.currentStoredValue", is("36")));

        // Invalid value on PUT -> 400 Bad Request (Alt A2)
        mockMvc.perform(put("/api/v1/configurations/booking.cancellation.min.hours")
                        .header("X-Actor-Id", "admin-1")
                        .header("X-Actor-Role", "ADMINISTRATOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proposedValue\":\"abc\",\"expectedVersion\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_VALUE")));

        // Unknown parameter -> 404 Not Found
        mockMvc.perform(get("/api/v1/configurations/does.not.exist")
                        .header("X-Actor-Id", "admin-1")
                        .header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("ExceptionHandler maps ConfigurationSaveException to HTTP 500 with previous value")
    void saveExceptionMapsToHttp500() {
        ConfigurationExceptionHandler handler = new ConfigurationExceptionHandler();
        var response = handler.handleSaveFailure(
                new ConfigurationSaveException("triage.urgent.sla.hours", "4", new RuntimeException("IO error"))
        );
        org.junit.jupiter.api.Assertions.assertEquals(500, response.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertEquals("4", response.getBody().get("previousValue"));
    }
}
