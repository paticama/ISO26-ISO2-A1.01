package com.sescam.radiodiagnostics.server.security;

import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test of UC-ADM-03 over HTTP: real C1 services wired by Spring.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuditRestControllerTest {

    private static final String FROM = "2026-11-20T00:00:00Z";
    private static final String TO = "2026-11-20T23:59:59Z";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditService auditService;

    @BeforeEach
    void recordSampleEvents() {
        auditService.recordEvent(new AuditEventDTO("CLINICAL_DATA_ACCESSED", "physician-7", "patient-42",
                null, null, null, Instant.parse("2026-11-20T09:00:00Z")));
        auditService.recordEvent(new AuditEventDTO("CONFIGURATION_PARAMETER_UPDATED", "admin-1",
                "booking.cancellation.min.hours", "24", "48", null, Instant.parse("2026-11-20T08:00:00Z")));
    }

    @Test
    @DisplayName("Authorized administrator gets time-ordered events with actor, action, timestamp and target")
    void searchReturnsOrderedEvents() throws Exception {
        mockMvc.perform(get("/api/v1/audit/events").param("from", FROM).param("to", TO)
                        .header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("MATCHES_FOUND")))
                .andExpect(jsonPath("$.totalMatches", is(2)))
                .andExpect(jsonPath("$.events[0].action", is("CONFIGURATION_PARAMETER_UPDATED")))
                .andExpect(jsonPath("$.events[0].actorId", is("admin-1")))
                .andExpect(jsonPath("$.events[0].timestamp", is("2026-11-20T08:00:00Z")))
                .andExpect(jsonPath("$.events[1].resourceKey", is("patient-42")));
    }

    @Test
    @DisplayName("Filters by target; empty result returns 200 with NO_MATCHES")
    void filtersAndEmptyResult() throws Exception {
        mockMvc.perform(get("/api/v1/audit/events").param("from", FROM).param("to", TO)
                        .param("resourceKey", "patient-42")
                        .header("X-Actor-Id", "auditor-1").header("X-Actor-Role", "COMPLIANCE_REVIEWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events", hasSize(1)));

        mockMvc.perform(get("/api/v1/audit/events").param("from", FROM).param("to", TO)
                        .param("performedBy", "nobody")
                        .header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("NO_MATCHES")))
                .andExpect(jsonPath("$.events", hasSize(0)));
    }

    @Test
    @DisplayName("A1: unauthorized role gets 403")
    void unauthorizedIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/audit/events").param("from", FROM).param("to", TO)
                        .header("X-Actor-Id", "patient-1").header("X-Actor-Role", "PATIENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));

        mockMvc.perform(get("/api/v1/audit/events/export").param("from", FROM).param("to", TO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A3: invalid, malformed and too broad queries are 400/422 with an explanation")
    void invalidAndTooBroadQueries() throws Exception {
        mockMvc.perform(get("/api/v1/audit/events").param("from", TO).param("to", FROM)
                        .header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_QUERY")));

        mockMvc.perform(get("/api/v1/audit/events").param("from", "yesterday").param("to", TO)
                        .header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("from")));

        mockMvc.perform(get("/api/v1/audit/events").param("from", "2026-01-01T00:00:00Z").param("to", TO)
                        .header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error", is("QUERY_TOO_BROAD")))
                .andExpect(jsonPath("$.limit", is(31)));
    }

    @Test
    @DisplayName("Step 5: export as CSV attachment")
    void exportCsv() throws Exception {
        mockMvc.perform(get("/api/v1/audit/events/export").param("from", FROM).param("to", TO)
                        .header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("audit-events.csv")))
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(content -> {
                    String body = content.getResponse().getContentAsString();
                    org.assertj.core.api.Assertions.assertThat(body.split("\n")).hasSize(3);
                });
    }

    @Test
    @DisplayName("Audit records cannot be deleted through the API")
    void noDeleteEndpoint() throws Exception {
        mockMvc.perform(delete("/api/v1/audit/events")
                        .header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(status().isMethodNotAllowed());
    }
}
