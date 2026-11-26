package com.sescam.radiodiagnostics.server.diagnostics;

import com.sescam.radiodiagnostics.common.diagnostics.DiagnosticReportingService;
import com.sescam.radiodiagnostics.common.diagnostics.DicomStudyDTO;
import com.sescam.radiodiagnostics.common.diagnostics.FinalizeReportRequestDTO;
import com.sescam.radiodiagnostics.common.diagnostics.IssueReportAddendumRequestDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ReportDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ReportableProcedureDTO;
import com.sescam.radiodiagnostics.common.diagnostics.SaveReportDraftRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for UC-PHY-04: Author, Edit, and Review Diagnostic Reports and Medical Images.
 */
@RestController
@RequestMapping("/api/v1/reports")
public class DiagnosticReportRestController {

    private final DiagnosticReportingService reportingService;

    public DiagnosticReportRestController(DiagnosticReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ReportableProcedureDTO>> pending(
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.ok(reportingService.listProceduresPendingReport(actorId, actorRole));
    }

    @GetMapping("/executions/{executionId}/study")
    public ResponseEntity<DicomStudyDTO> dicomStudy(
            @PathVariable("executionId") String executionId,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.ok(reportingService.getDicomStudy(executionId, actorId, actorRole));
    }

    @GetMapping("/executions/{executionId}")
    public ResponseEntity<ReportDTO> byExecution(
            @PathVariable("executionId") String executionId,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.of(reportingService.findReportByExecutionId(executionId, actorId, actorRole));
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ReportDTO> byId(
            @PathVariable("reportId") String reportId,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.ok(reportingService.getReportById(reportId, actorId, actorRole));
    }

    @PutMapping("/draft")
    public ResponseEntity<ReportDTO> saveDraft(
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestBody DraftPayload payload) {
        SaveReportDraftRequestDTO request = new SaveReportDraftRequestDTO(
                payload.executionId(),
                payload.findings(),
                payload.conclusion(),
                Boolean.TRUE.equals(payload.criticalAlert()),
                Boolean.TRUE.equals(payload.followUpRecommended()),
                payload.expectedVersion(),
                actorId,
                actorRole
        );
        return ResponseEntity.ok(reportingService.saveDraft(request));
    }

    @PostMapping("/finalize")
    public ResponseEntity<ReportDTO> finalizeReport(
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestBody FinalizePayload payload) {
        FinalizeReportRequestDTO request = new FinalizeReportRequestDTO(
                payload.executionId(),
                payload.findings(),
                payload.conclusion(),
                Boolean.TRUE.equals(payload.criticalAlert()),
                Boolean.TRUE.equals(payload.followUpRecommended()),
                payload.digitalSignature(),
                payload.expectedVersion(),
                actorId,
                actorRole
        );
        return ResponseEntity.ok(reportingService.finalizeAndSign(request));
    }

    @PostMapping("/{reportId}/addendums")
    public ResponseEntity<ReportDTO> issueAddendum(
            @PathVariable("reportId") String reportId,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestBody AddendumPayload payload) {
        IssueReportAddendumRequestDTO request = new IssueReportAddendumRequestDTO(
                reportId,
                payload.reason(),
                payload.addendumContent(),
                payload.digitalSignature(),
                payload.expectedVersion(),
                actorId,
                actorRole
        );
        return ResponseEntity.ok(reportingService.issueAddendum(request));
    }

    public record DraftPayload(
            String executionId,
            String findings,
            String conclusion,
            Boolean criticalAlert,
            Boolean followUpRecommended,
            Long expectedVersion
    ) {
    }

    public record FinalizePayload(
            String executionId,
            String findings,
            String conclusion,
            Boolean criticalAlert,
            Boolean followUpRecommended,
            String digitalSignature,
            Long expectedVersion
    ) {
    }

    public record AddendumPayload(
            String reason,
            String addendumContent,
            String digitalSignature,
            Long expectedVersion
    ) {
    }
}
