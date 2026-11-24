package com.sescam.radiodiagnostics.client.diagnostics;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.diagnostics.ClinicalIncidentDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionRecordDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionStatus;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentSeverity;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentType;
import com.sescam.radiodiagnostics.common.diagnostics.ScheduledProcedureDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ExecutionViewController.class)
@TestPropertySource(properties = "sescam.client.zone=UTC")
class ExecutionViewControllerTest {

    private static final Instant START = Instant.parse("2026-11-24T09:05:00Z");
    private static final Instant END = Instant.parse("2026-11-24T09:35:00Z");
    private static final ScheduledProcedureDTO CT = new ScheduledProcedureDTO("ORD-DEMO-CT", "PAT-DEMO-2", "CT-ABD-C",
            "Abdominal CT with contrast", ModalityType.CT, Instant.parse("2026-11-24T09:00:00Z"), "CT-ROOM-1", true, true);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExecutionApiClient apiClient;

    @BeforeEach
    void pending() {
        when(apiClient.listPending(any(), any())).thenReturn(List.of(CT));
    }

    private static MockHttpServletRequestBuilder submitCt() {
        return post("/executions").header("X-Actor-Id", "tech-3").header("X-Actor-Role", "RADIOLOGY_TECHNICIAN")
                .param("orderId", "ORD-DEMO-CT").param("startedAt", "2026-11-24T09:05").param("endedAt", "2026-11-24T09:35")
                .param("roomEquipmentId", "CT-ROOM-1").param("contrastBatch", "IOP-117").param("contrastVolumeMl", "90")
                .param("doseDlp", "520").param("doseCtdiVol", "14.2").param("informedConsentVerified", "true");
    }

    private static ExecutionRecordDTO record(ExecutionStatus status, String reason, List<ClinicalIncidentDTO> incidents) {
        return new ExecutionRecordDTO("EXE-1", "ORD-DEMO-CT", "PAT-DEMO-2", "Abdominal CT with contrast",
                ModalityType.CT, START, END, "CT-ROOM-1", "IOP-117", 90.0, 520.0, 14.2, null, true, status, reason,
                incidents, "tech-3", END);
    }

    private static ClinicalIncidentDTO incident(boolean aborted) {
        return new ClinicalIncidentDTO("INC-1", "EXE-1", "PAT-DEMO-2", IncidentType.ADVERSE_CONTRAST_REACTION,
                IncidentSeverity.SEVERE, "Reaction", null, END, aborted);
    }

    @Test
    void step1ShowsTheProceduresAwaitingExecution() throws Exception {
        mockMvc.perform(get("/executions/new").header("X-Actor-Id", "tech-3").header("X-Actor-Role", "RADIOLOGY_TECHNICIAN"))
                .andExpect(status().isOk())
                .andExpect(view().name("executions/new"))
                .andExpect(content().string(containsString("Abdominal CT with contrast – patient PAT-DEMO-2 – CT-ROOM-1 – 2026-11-24 09:00")))
                .andExpect(content().string(containsString("ADVERSE_CONTRAST_REACTION")))
                .andExpect(content().string(containsString("SEVERE")));
        verify(apiClient).listPending("tech-3", "RADIOLOGY_TECHNICIAN");
    }

    @Test
    void emptyListAndPendingFailureAreShown() throws Exception {
        when(apiClient.listPending(any(), any())).thenReturn(List.of());
        mockMvc.perform(get("/executions/new"))
                .andExpect(content().string(containsString("id=\"none-pending\"")));

        when(apiClient.listPending(any(), any())).thenThrow(new ExecutionClientException(
                ExecutionClientException.Kind.FORBIDDEN, "You are not authorized to log procedure executions."));
        mockMvc.perform(get("/executions/new"))
                .andExpect(content().string(containsString("id=\"pending-failure\"")))
                .andExpect(content().string(containsString("You are not authorized")));
    }

    @Test
    void mainFlowRecordsAndConfirms() throws Exception {
        when(apiClient.recordExecution(any(), eq("tech-3"), eq("RADIOLOGY_TECHNICIAN")))
                .thenReturn(record(ExecutionStatus.EXECUTED_PENDING_REPORT, null, List.of()));

        mockMvc.perform(submitCt())
                .andExpect(content().string(containsString("id=\"recorded\"")))
                .andExpect(content().string(containsString("Execution recorded.")))
                .andExpect(content().string(containsString("2026-11-24 09:05 – 09:35, CT-ROOM-1")))
                .andExpect(content().string(not(containsString("id=\"execution-form\""))))
                .andExpect(content().string(not(containsString("id=\"incidents-logged\""))));

        org.mockito.ArgumentCaptor<ExecutionApiClient.ExecutionPayload> payload =
                org.mockito.ArgumentCaptor.forClass(ExecutionApiClient.ExecutionPayload.class);
        verify(apiClient).recordExecution(payload.capture(), eq("tech-3"), eq("RADIOLOGY_TECHNICIAN"));
        assertThat(payload.getValue().startedAt()).isEqualTo(START);
        assertThat(payload.getValue().doseCtdiVol()).isEqualTo(14.2);
        assertThat(payload.getValue().incidents()).isEmpty();
    }

    @Test
    void alternative2aIncidentIsSentAndReported() throws Exception {
        when(apiClient.recordExecution(any(), any(), any()))
                .thenReturn(record(ExecutionStatus.EXECUTED_PENDING_REPORT, null, List.of(incident(false))));

        mockMvc.perform(submitCt().param("incidentType", "ADVERSE_CONTRAST_REACTION").param("incidentSeverity", "MODERATE")
                        .param("incidentDescription", "Urticaria").param("incidentTreatment", "Antihistamine"))
                .andExpect(content().string(containsString("id=\"incidents-logged\"")))
                .andExpect(content().string(containsString("1 incident(s) logged and flagged for audit.")));
    }

    @Test
    void alternative2bAbortShowsTheReschedulingNotice() throws Exception {
        when(apiClient.recordExecution(any(), any(), any()))
                .thenReturn(record(ExecutionStatus.INCOMPLETE_INCIDENT, "Severe reaction", List.of(incident(true))));

        mockMvc.perform(submitCt().param("aborted", "true").param("abortReason", "Severe reaction")
                        .param("incidentType", "ANAPHYLACTOID_EVENT").param("incidentSeverity", "SEVERE")
                        .param("incidentDescription", "Anaphylactoid reaction"))
                .andExpect(content().string(containsString("id=\"aborted\"")))
                .andExpect(content().string(containsString("Rescheduling must be requested to Admissions manually.")))
                .andExpect(content().string(containsString("Reason: Severe reaction")));
    }

    @Test
    void invalidFormIsNotSentToTheServer() throws Exception {
        mockMvc.perform(post("/executions").param("orderId", "ORD-DEMO-CT").param("aborted", "true"))
                .andExpect(content().string(containsString("Enter the actual start.")))
                .andExpect(content().string(containsString("Give the reason the procedure was aborted.")))
                .andExpect(content().string(containsString("Describe the incident.")))
                .andExpect(content().string(containsString("id=\"execution-form\"")));
        verify(apiClient, never()).recordExecution(any(), any(), any());
    }

    @Test
    void serverFieldErrorsAreShownOnTheirFields() throws Exception {
        when(apiClient.recordExecution(any(), any(), any())).thenThrow(new ExecutionClientException(
                ExecutionClientException.Kind.INVALID_DATA, "The execution log is not valid.",
                List.of("informedConsentVerified", "doseDlp", "incidents[0].severity", "incidents"), null));

        mockMvc.perform(submitCt())
                .andExpect(content().string(containsString("id=\"failure\"")))
                .andExpect(content().string(containsString("This examination requires verified informed consent.")))
                .andExpect(content().string(containsString("Mandatory for CT examinations.")))
                .andExpect(content().string(containsString("This field is mandatory or not valid.")));
    }

    @Test
    void retryableAndNonRetryableFailures() throws Exception {
        when(apiClient.recordExecution(any(), any(), any())).thenThrow(new ExecutionClientException(
                ExecutionClientException.Kind.ALREADY_RECORDED, "The execution of this procedure is already recorded."));
        mockMvc.perform(submitCt())
                .andExpect(content().string(containsString("already recorded")))
                .andExpect(content().string(not(containsString("You can send it again."))));

        doThrow(new ExecutionClientException(ExecutionClientException.Kind.SERVER_UNREACHABLE,
                "The server cannot be reached.")).when(apiClient).recordExecution(any(), any(), any());
        mockMvc.perform(submitCt())
                .andExpect(content().string(containsString("The server cannot be reached.")))
                .andExpect(content().string(containsString("You can send it again.")))
                .andExpect(content().string(containsString("value=\"CT-ROOM-1\"")));
    }

    @Test
    void serverFieldNamesAreMappedToTheFormFields() {
        assertThat(ExecutionViewController.serverFieldErrors(List.of(
                "incidents[0].type", "incidents[0].description", "radiotracerActivityMBq", "contrastVolumeMl")))
                .containsOnlyKeys("incidentType", "incidentDescription", "radiotracerActivityMBq", "contrastVolumeMl")
                .containsEntry("radiotracerActivityMBq", "Mandatory for nuclear medicine examinations.")
                .containsEntry("contrastVolumeMl", "This examination uses contrast: enter the volume administered.");
    }
}
