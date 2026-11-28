package com.sescam.radiodiagnostics.server.diagnostics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sescam.radiodiagnostics.common.diagnostics.EhrReportSyncStatus;
import com.sescam.radiodiagnostics.common.diagnostics.FhirDiagnosticReportGateway;
import com.sescam.radiodiagnostics.common.diagnostics.PacsConnectionException;
import com.sescam.radiodiagnostics.common.diagnostics.PacsGateway;
import com.sescam.radiodiagnostics.common.diagnostics.ReportAvailabilityEvent;
import com.sescam.radiodiagnostics.common.diagnostics.ReportEventPublisher;
import com.sescam.radiodiagnostics.diagnostics.internal.InMemoryPacsGateway;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DiagnosticReportRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PacsGateway pacsGateway;

    @MockitoBean
    private FhirDiagnosticReportGateway fhirGateway;

    @MockitoBean
    private ReportEventPublisher reportEventPublisher;

    private String recordSampleExecution() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/executions")
                        .header("X-Actor-Id", "tech-3")
                        .header("X-Actor-Role", "RADIOLOGY_TECHNICIAN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"ORD-DEMO-CT","startedAt":"2026-11-26T09:05:00Z",
                                 "endedAt":"2026-11-26T09:35:00Z","roomEquipmentId":"CT-ROOM-1",
                                 "contrastBatch":"IOP-117","contrastVolumeMl":90,
                                 "doseDlp":520,"doseCtdiVol":14.2,"informedConsentVerified":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("executionId").asText();
    }

    @Test
    @DisplayName("Full UC-PHY-04 lifecycle: worklist -> DICOM study -> draft -> finalize -> addendum")
    void fullReportingLifecycle() throws Exception {
        String executionId = recordSampleExecution();
        InMemoryPacsGateway realPacs = new InMemoryPacsGateway();
        when(pacsGateway.fetchStudyForExecution(any())).thenAnswer(inv -> realPacs.fetchStudyForExecution(inv.getArgument(0)));
        when(fhirGateway.exportDiagnosticReport(any())).thenReturn(EhrReportSyncStatus.SYNCED);

        mockMvc.perform(get("/api/v1/reports/pending")
                        .header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].executionId", is(executionId)));

        mockMvc.perform(get("/api/v1/reports/executions/{executionId}/study", executionId)
                        .header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId", is(executionId)))
                .andExpect(jsonPath("$.wadoViewerToken", is("wado-" + executionId.toLowerCase())));

        mockMvc.perform(get("/api/v1/reports/executions/{executionId}", executionId)
                        .header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN"))
                .andExpect(status().isNotFound());

        MvcResult draftRes = mockMvc.perform(put("/api/v1/reports/draft")
                        .header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"executionId":"%s","findings":"Initial findings","conclusion":"Normal",
                                 "criticalAlert":false,"followUpRecommended":false}
                                """.formatted(executionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.ehrSyncStatus", is("NOT_SYNCED")))
                .andReturn();
        String reportId = objectMapper.readTree(draftRes.getResponse().getContentAsString()).get("reportId").asText();

        verify(fhirGateway, never()).exportDiagnosticReport(any());
        verify(reportEventPublisher, never()).publishReportAvailable(any());

        mockMvc.perform(post("/api/v1/reports/finalize")
                        .header("X-Actor-Id", "dr-1").header("X-Actor-Role", "RADIOLOGIST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"executionId":"%s","findings":"Focal hepatic lesion","conclusion":"Urgent oncology review",
                                 "criticalAlert":true,"followUpRecommended":true,"digitalSignature":"SIG-DR1","expectedVersion":1}
                                """.formatted(executionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FINALIZED")))
                .andExpect(jsonPath("$.ehrSyncStatus", is("SYNCED")))
                .andExpect(jsonPath("$.digitalSignature", is("SIG-DR1")))
                .andExpect(jsonPath("$.criticalAlert", is(true)));

        mockMvc.perform(get("/api/v1/reports/{reportId}", reportId)
                        .header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId", is(reportId)));

        mockMvc.perform(post("/api/v1/reports/{reportId}/addendums", reportId)
                        .header("X-Actor-Id", "dr-2").header("X-Actor-Role", "RADIOLOGIST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Comparison with prior CT","addendumContent":"Lesion increased by 4mm",
                                 "digitalSignature":"SIG-DR2","expectedVersion":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("AMENDED")))
                .andExpect(jsonPath("$.digitalSignature", is("SIG-DR1")))
                .andExpect(jsonPath("$.addendums", hasSize(1)));

        ArgumentCaptor<ReportAvailabilityEvent> events = ArgumentCaptor.forClass(ReportAvailabilityEvent.class);
        verify(reportEventPublisher, org.mockito.Mockito.times(2)).publishReportAvailable(events.capture());
        assertThat(events.getAllValues()).extracting(ReportAvailabilityEvent::addendum).containsExactly(false, true);
    }

    @Test
    @DisplayName("Error mappings: 403 RBAC, 400 validation, 404 missing, 409 conflict, 502 PACS failure")
    void mapsAllDomainExceptionsToHttpStatuses() throws Exception {
        String executionId = recordSampleExecution();

        mockMvc.perform(get("/api/v1/reports/pending")
                        .header("X-Actor-Id", "tech-3").header("X-Actor-Role", "RADIOLOGY_TECHNICIAN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));

        when(pacsGateway.fetchStudyForExecution(any())).thenThrow(new PacsConnectionException("PACS offline"));
        mockMvc.perform(get("/api/v1/reports/executions/{executionId}/study", executionId)
                        .header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error", is("PACS_CONNECTION_ERROR")));

        mockMvc.perform(post("/api/v1/reports/finalize")
                        .header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"executionId":"%s","findings":"","conclusion":"","digitalSignature":""}
                                """.formatted(executionId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_DATA")))
                .andExpect(jsonPath("$.invalidFields", hasItem("digitalSignature")));

        mockMvc.perform(get("/api/v1/reports/REP-NOPE")
                        .header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("REPORT_NOT_FOUND")));

        when(fhirGateway.exportDiagnosticReport(any())).thenReturn(EhrReportSyncStatus.SYNCED);
        mockMvc.perform(post("/api/v1/reports/finalize")
                        .header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"executionId":"%s","findings":"F","conclusion":"C","digitalSignature":"SIG"}
                                """.formatted(executionId)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/reports/draft")
                        .header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"executionId":"%s","findings":"F2","conclusion":"C2"}
                                """.formatted(executionId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("REPORT_STATE_CONFLICT")));
    }
}
