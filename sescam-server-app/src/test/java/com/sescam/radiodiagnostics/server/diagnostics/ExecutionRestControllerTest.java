package com.sescam.radiodiagnostics.server.diagnostics;

import com.sescam.radiodiagnostics.common.diagnostics.ProcedureEventPublisher;
import com.sescam.radiodiagnostics.common.diagnostics.ProcedureExecutionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ExecutionRestControllerTest {

    private static final String CT_COMPLETED = """
            {"orderId":"ORD-DEMO-CT","startedAt":"2026-11-20T09:05:00Z","endedAt":"2026-11-20T09:35:00Z",
             "roomEquipmentId":"CT-ROOM-1","contrastBatch":"IOP-117","contrastVolumeMl":90,
             "doseDlp":520,"doseCtdiVol":14.2,"informedConsentVerified":true}
            """;

    private static final String CT_ABORTED = """
            {"orderId":"ORD-DEMO-CT","startedAt":"2026-11-20T09:05:00Z","endedAt":"2026-11-20T09:21:00Z",
             "roomEquipmentId":"CT-ROOM-1","informedConsentVerified":true,"aborted":true,
             "abortReason":"Severe contrast reaction",
             "incidents":[{"type":"ADVERSE_CONTRAST_REACTION","severity":"SEVERE",
                           "description":"Anaphylactoid reaction","treatmentGiven":"Adrenaline"}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcedureEventPublisher eventPublisher;

    private static MockHttpServletRequestBuilder postAs(String role, String body) {
        return post("/api/v1/executions").header("X-Actor-Id", "tech-3").header("X-Actor-Role", role)
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    @DisplayName("GET /api/v1/executions/pending lists booked procedures not yet executed (step 1)")
    void pending() throws Exception {
        mockMvc.perform(get("/api/v1/executions/pending").header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].orderId", hasItem("ORD-DEMO-CT")));

        mockMvc.perform(postAs("RADIOLOGY_TECHNICIAN", CT_COMPLETED)).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/executions/pending").header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN"))
                .andExpect(jsonPath("$[*].orderId", not(hasItem("ORD-DEMO-CT"))));
    }

    @Test
    @DisplayName("POST records the execution with its metrics (201, EXECUTED_PENDING_REPORT)")
    void recordsExecution() throws Exception {
        mockMvc.perform(postAs("RADIOLOGY_TECHNICIAN", CT_COMPLETED))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("EXECUTED_PENDING_REPORT")))
                .andExpect(jsonPath("$.patientId", is("PAT-DEMO-2")))
                .andExpect(jsonPath("$.doseDlp", is(520.0)))
                .andExpect(jsonPath("$.contrastBatch", is("IOP-117")))
                .andExpect(jsonPath("$.recordedBy", is("tech-3")));
        verify(eventPublisher).publishExecutionRecorded(any());
    }

    @Test
    @DisplayName("2a/2b: aborted with incident returns INCOMPLETE_INCIDENT and asks admissions to reschedule")
    void abortsWithIncident() throws Exception {
        mockMvc.perform(postAs("PHYSICIAN", CT_ABORTED))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("INCOMPLETE_INCIDENT")))
                .andExpect(jsonPath("$.incidents", hasSize(1)))
                .andExpect(jsonPath("$.incidents[0].severity", is("SEVERE")))
                .andExpect(jsonPath("$.incidents[0].patientId", is("PAT-DEMO-2")))
                .andExpect(jsonPath("$.incidents[0].procedureAborted", is(true)));

        ArgumentCaptor<ProcedureExecutionEvent> event = ArgumentCaptor.forClass(ProcedureExecutionEvent.class);
        verify(eventPublisher).publishExecutionRecorded(event.capture());
        assertThat(event.getValue().requiresRescheduling()).isTrue();
    }

    @Test
    @DisplayName("Invalid data returns 400 with the invalid fields; empty incident entries too")
    void invalidData() throws Exception {
        mockMvc.perform(postAs("PHYSICIAN", """
                        {"orderId":"ORD-DEMO-CT","startedAt":"2026-11-20T09:05:00Z","endedAt":"2026-11-20T09:35:00Z",
                         "roomEquipmentId":"CT-ROOM-1"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_DATA")))
                .andExpect(jsonPath("$.invalidFields", hasItem("informedConsentVerified")))
                .andExpect(jsonPath("$.invalidFields", hasItem("doseDlp")));

        mockMvc.perform(postAs("PHYSICIAN", """
                        {"orderId":"ORD-DEMO-XR","startedAt":"2026-11-20T09:05:00Z","endedAt":"2026-11-20T09:35:00Z",
                         "roomEquipmentId":"XR-1","incidents":[null]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidFields", hasItem("incidents[0].type")))
                .andExpect(jsonPath("$.invalidFields", hasItem("incidents[0].description")));
        verify(eventPublisher, never()).publishExecutionRecorded(any());
    }

    @Test
    @DisplayName("The role is checked before the data: an empty incident from another role gets 403, not 400 (#188)")
    void roleCheckedBeforeData() throws Exception {
        mockMvc.perform(postAs("ADMINISTRATIVE_STAFF", """
                        {"orderId":"ORD-DEMO-XR","incidents":[null]}
                        """))
                .andExpect(status().isForbidden());
        verify(eventPublisher, never()).publishExecutionRecorded(any());
    }

    @Test
    @DisplayName("403 for other roles, 404 for unbooked orders, 409 for a second record")
    void errors() throws Exception {
        mockMvc.perform(postAs("ADMINISTRATIVE_STAFF", CT_COMPLETED)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/executions/pending")).andExpect(status().isForbidden());

        mockMvc.perform(postAs("PHYSICIAN", CT_COMPLETED.replace("ORD-DEMO-CT", "ORD-NOPE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("PROCEDURE_NOT_FOUND")));

        mockMvc.perform(postAs("PHYSICIAN", CT_COMPLETED)).andExpect(status().isCreated());
        mockMvc.perform(postAs("PHYSICIAN", CT_COMPLETED))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("ALREADY_RECORDED")));
    }
}
