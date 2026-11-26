package com.sescam.radiodiagnostics.client.diagnostics;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.diagnostics.DicomStudyDTO;
import com.sescam.radiodiagnostics.common.diagnostics.EhrReportSyncStatus;
import com.sescam.radiodiagnostics.common.diagnostics.ReportAddendumDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ReportDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ReportStatus;
import com.sescam.radiodiagnostics.common.diagnostics.ReportableProcedureDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(DiagnosticReportViewController.class)
@TestPropertySource(properties = "sescam.client.zone=UTC")
class DiagnosticReportViewControllerTest {

    private static final Instant T = Instant.parse("2026-11-26T09:25:00Z");
    private static final DicomStudyDTO STUDY = new DicomStudyDTO(
            "1.2.3.4", "EXE-1", "ORD-1", "PAT-1", "ACC-1", ModalityType.CT,
            "Abdominal CT", 4, 180, "wado-exe-1", "https://pacs.sescam.local/wado-rs/studies/1.2.3.4", T);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiagnosticReportApiClient apiClient;

    private static ReportDTO report(ReportStatus status, EhrReportSyncStatus syncStatus, boolean critical, boolean followUp,
                                    List<ReportAddendumDTO> addendums) {
        return new ReportDTO("REP-1", "EXE-1", "ORD-1", "PAT-1", "1.2.3.4", "Abdominal CT", ModalityType.CT,
                "Findings text", "Conclusion text", critical, followUp, status, "dr-1",
                status == ReportStatus.DRAFT ? null : "SIG-1", status == ReportStatus.DRAFT ? null : T,
                syncStatus, syncStatus == EhrReportSyncStatus.SYNCED ? T : null, 2L, T, T, addendums);
    }

    @Test
    void worklistShowsPendingProceduresAndErrors() throws Exception {
        when(apiClient.listPending("dr-1", "PHYSICIAN")).thenReturn(List.of(
                new ReportableProcedureDTO("EXE-1", "ORD-1", "PAT-1", "Abdominal CT", ModalityType.CT, T, null, null)));

        mockMvc.perform(get("/reports").header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN"))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/worklist"))
                .andExpect(content().string(containsString("EXE-1")))
                .andExpect(content().string(containsString("PENDING")));

        when(apiClient.listPending(any(), any())).thenThrow(new DiagnosticReportClientException(
                DiagnosticReportClientException.Kind.FORBIDDEN, "Not authorized"));
        mockMvc.perform(get("/reports"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"worklist-failure\"")));
    }

    @Test
    void editorRendersDicomViewerAndHandlesPacsFailure() throws Exception {
        when(apiClient.getDicomStudy("EXE-1", "dr-1", "PHYSICIAN")).thenReturn(STUDY);
        when(apiClient.findReportByExecutionId("EXE-1", "dr-1", "PHYSICIAN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/reports/executions/EXE-1").header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN"))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/editor"))
                .andExpect(content().string(containsString("wado-exe-1")))
                .andExpect(content().string(containsString("id=\"report-editor\"")));

        when(apiClient.getDicomStudy("EXE-1", "dr-1", "PHYSICIAN")).thenThrow(new DiagnosticReportClientException(
                DiagnosticReportClientException.Kind.PACS_ERROR, "PACS unreachable"));
        mockMvc.perform(get("/reports/executions/EXE-1").header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"pacs-failure\"")))
                .andExpect(content().string(containsString("PACS unreachable")));
    }

    @Test
    void draftAndFinalizeAndAddendumWorkflows() throws Exception {
        when(apiClient.getDicomStudy("EXE-1", "dr-1", "PHYSICIAN")).thenReturn(STUDY);
        ReportDTO draft = report(ReportStatus.DRAFT, EhrReportSyncStatus.NOT_SYNCED, false, false, List.of());
        when(apiClient.saveDraft(any(), eq("dr-1"), eq("PHYSICIAN"))).thenReturn(draft);

        mockMvc.perform(post("/reports/executions/EXE-1/draft")
                        .header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN")
                        .param("findings", "Draft findings").param("conclusion", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"draft-saved\"")));

        mockMvc.perform(post("/reports/executions/EXE-1/finalize")
                        .header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN")
                        .param("findings", "").param("conclusion", "").param("digitalSignature", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Digital signature is mandatory")));

        ReportDTO finalized = report(ReportStatus.FINALIZED, EhrReportSyncStatus.SYNCED, true, true, List.of());
        when(apiClient.finalizeAndSign(any(), eq("dr-1"), eq("PHYSICIAN"))).thenReturn(finalized);
        mockMvc.perform(post("/reports/executions/EXE-1/finalize")
                        .header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN")
                        .param("findings", "Findings text").param("conclusion", "Conclusion text")
                        .param("criticalAlert", "true").param("followUpRecommended", "true")
                        .param("digitalSignature", "SIG-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"report-finalized\"")))
                .andExpect(content().string(containsString("id=\"critical-alert-banner\"")))
                .andExpect(content().string(containsString("id=\"follow-up-hint\"")));

        when(apiClient.getReportById("REP-1", "dr-1", "PHYSICIAN")).thenReturn(finalized);
        ReportDTO amended = report(ReportStatus.AMENDED, EhrReportSyncStatus.SYNCED, true, true,
                List.of(new ReportAddendumDTO("ADD-1", "Clarification", "Added note", "dr-1", "SIG-2", T)));
        when(apiClient.issueAddendum(eq("REP-1"), any(), eq("dr-1"), eq("PHYSICIAN"))).thenReturn(amended);

        mockMvc.perform(post("/reports/REP-1/addendums")
                        .header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN")
                        .param("reason", "Clarification").param("addendumContent", "Added note")
                        .param("digitalSignature", "SIG-2").param("expectedVersion", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"addendum-saved\"")))
                .andExpect(content().string(containsString("Added note")));

        verify(apiClient).issueAddendum(eq("REP-1"), any(), eq("dr-1"), eq("PHYSICIAN"));
    }

    @Test
    void mapsServerValidationAndConflictErrorsOnPost() throws Exception {
        when(apiClient.getDicomStudy("EXE-1", "dr-1", "PHYSICIAN")).thenReturn(STUDY);
        when(apiClient.findReportByExecutionId("EXE-1", "dr-1", "PHYSICIAN")).thenReturn(Optional.empty());
        when(apiClient.finalizeAndSign(any(), eq("dr-1"), eq("PHYSICIAN"))).thenThrow(
                new DiagnosticReportClientException(DiagnosticReportClientException.Kind.INVALID_DATA,
                        "Invalid", List.of("findings", "digitalSignature"), null));

        mockMvc.perform(post("/reports/executions/EXE-1/finalize")
                        .header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN")
                        .param("findings", "F").param("conclusion", "C").param("digitalSignature", "S"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"failure\"")))
                .andExpect(content().string(containsString("Clinical findings are mandatory before signing.")));
    }
}
