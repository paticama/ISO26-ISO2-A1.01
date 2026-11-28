package com.sescam.radiodiagnostics.client.diagnostics;

import com.sescam.radiodiagnostics.common.diagnostics.DicomStudyDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ReportDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Web UI controller for UC-PHY-04: reporting worklist, DICOM study viewer, draft/finalized report editor,
 * and post-signature addendums.
 */
@Controller
@RequestMapping("/reports")
public class DiagnosticReportViewController {

    static final String WORKLIST_VIEW = "reports/worklist";
    static final String EDITOR_VIEW = "reports/editor";

    private final DiagnosticReportApiClient apiClient;
    private final ZoneId zone;

    public DiagnosticReportViewController(DiagnosticReportApiClient apiClient,
                                          @Value("${sescam.client.zone}") String zone) {
        this.apiClient = apiClient;
        this.zone = ZoneId.of(zone);
    }

    @GetMapping
    public String worklist(Model model,
                           @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                           @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        model.addAttribute("zone", zone);
        try {
            model.addAttribute("procedures", apiClient.listPending(actorId, actorRole));
        } catch (DiagnosticReportClientException ex) {
            model.addAttribute("procedures", List.of());
            model.addAttribute("worklistFailure", ex.getMessage());
        }
        return WORKLIST_VIEW;
    }

    @GetMapping("/executions/{executionId}")
    public String editor(@PathVariable("executionId") String executionId,
                         Model model,
                         @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                         @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        loadEditorContext(executionId, null, null, null, model, actorId, actorRole);
        return EDITOR_VIEW;
    }

    @PostMapping("/executions/{executionId}/draft")
    public String saveDraft(@PathVariable("executionId") String executionId,
                            @ModelAttribute("reportForm") ReportForm form,
                            Model model,
                            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        form.setExecutionId(executionId);
        Map<String, String> errors = form.validateDraft();
        if (!errors.isEmpty()) {
            loadEditorContext(executionId, null, form, null, model, actorId, actorRole);
            model.addAttribute("errors", errors);
            return EDITOR_VIEW;
        }
        try {
            ReportDTO saved = apiClient.saveDraft(form.toDraftPayload(), actorId, actorRole);
            loadEditorContext(executionId, saved, ReportForm.fromReport(saved), null, model, actorId, actorRole);
            model.addAttribute("draftSaved", true);
        } catch (DiagnosticReportClientException ex) {
            loadEditorContext(executionId, null, form, null, model, actorId, actorRole);
            model.addAttribute("failure", ex);
            if (ex.getKind() == DiagnosticReportClientException.Kind.INVALID_DATA) {
                model.addAttribute("errors", serverFieldErrors(ex.getInvalidFields()));
            }
        }
        return EDITOR_VIEW;
    }

    @PostMapping("/executions/{executionId}/finalize")
    public String finalizeReport(@PathVariable("executionId") String executionId,
                                 @ModelAttribute("reportForm") ReportForm form,
                                 Model model,
                                 @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                                 @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        form.setExecutionId(executionId);
        Map<String, String> errors = form.validateFinalize();
        if (!errors.isEmpty()) {
            loadEditorContext(executionId, null, form, null, model, actorId, actorRole);
            model.addAttribute("errors", errors);
            return EDITOR_VIEW;
        }
        try {
            ReportDTO finalized = apiClient.finalizeAndSign(form.toFinalizePayload(), actorId, actorRole);
            loadEditorContext(executionId, finalized, ReportForm.fromReport(finalized), null, model, actorId, actorRole);
            model.addAttribute("finalizedSuccess", true);
        } catch (DiagnosticReportClientException ex) {
            loadEditorContext(executionId, null, form, null, model, actorId, actorRole);
            model.addAttribute("failure", ex);
            if (ex.getKind() == DiagnosticReportClientException.Kind.INVALID_DATA) {
                model.addAttribute("errors", serverFieldErrors(ex.getInvalidFields()));
            }
        }
        return EDITOR_VIEW;
    }

    @PostMapping("/{reportId}/addendums")
    public String issueAddendum(@PathVariable("reportId") String reportId,
                                @ModelAttribute("addendumForm") AddendumForm addendumForm,
                                Model model,
                                @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                                @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        addendumForm.setReportId(reportId);
        ReportDTO current = null;
        try {
            current = apiClient.getReportById(reportId, actorId, actorRole);
        } catch (DiagnosticReportClientException ex) {
            model.addAttribute("zone", zone);
            model.addAttribute("failure", ex);
            model.addAttribute("reportForm", new ReportForm());
            model.addAttribute("addendumForm", addendumForm);
            return EDITOR_VIEW;
        }

        Map<String, String> errors = addendumForm.validate();
        if (!errors.isEmpty()) {
            loadEditorContext(current.executionId(), current, ReportForm.fromReport(current), addendumForm, model, actorId, actorRole);
            model.addAttribute("addendumErrors", errors);
            return EDITOR_VIEW;
        }
        try {
            ReportDTO amended = apiClient.issueAddendum(reportId, addendumForm.toPayload(), actorId, actorRole);
            AddendumForm freshAddendum = new AddendumForm();
            freshAddendum.setReportId(amended.reportId());
            freshAddendum.setExpectedVersion(amended.version());
            loadEditorContext(amended.executionId(), amended, ReportForm.fromReport(amended), freshAddendum, model, actorId, actorRole);
            model.addAttribute("addendumSaved", true);
        } catch (DiagnosticReportClientException ex) {
            loadEditorContext(current.executionId(), current, ReportForm.fromReport(current), addendumForm, model, actorId, actorRole);
            model.addAttribute("failure", ex);
            if (ex.getKind() == DiagnosticReportClientException.Kind.INVALID_DATA) {
                model.addAttribute("addendumErrors", serverFieldErrors(ex.getInvalidFields()));
            }
        }
        return EDITOR_VIEW;
    }

    private void loadEditorContext(String executionId,
                                   ReportDTO knownReport,
                                   ReportForm submittedReportForm,
                                   AddendumForm submittedAddendumForm,
                                   Model model,
                                   String actorId,
                                   String actorRole) {
        model.addAttribute("zone", zone);
        model.addAttribute("executionId", executionId);

        try {
            DicomStudyDTO study = apiClient.getDicomStudy(executionId, actorId, actorRole);
            model.addAttribute("study", study);
        } catch (DiagnosticReportClientException ex) {
            model.addAttribute("pacsFailure", ex.getMessage());
        }

        ReportDTO report = knownReport;
        if (report == null) {
            try {
                Optional<ReportDTO> existing = apiClient.findReportByExecutionId(executionId, actorId, actorRole);
                report = existing.orElse(null);
            } catch (DiagnosticReportClientException ex) {
                model.addAttribute("reportLoadFailure", ex.getMessage());
            }
        }
        model.addAttribute("report", report);

        ReportForm reportForm = submittedReportForm;
        if (reportForm == null) {
            reportForm = report != null ? ReportForm.fromReport(report) : new ReportForm();
            reportForm.setExecutionId(executionId);
        }
        model.addAttribute("reportForm", reportForm);

        AddendumForm addendumForm = submittedAddendumForm;
        if (addendumForm == null) {
            addendumForm = new AddendumForm();
            if (report != null) {
                addendumForm.setReportId(report.reportId());
                addendumForm.setExpectedVersion(report.version());
            }
        }
        model.addAttribute("addendumForm", addendumForm);
    }

    static Map<String, String> serverFieldErrors(List<String> fields) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (String field : fields) {
            errors.put(field, switch (field) {
                case "findings" -> "Clinical findings are mandatory before signing.";
                case "conclusion" -> "Diagnostic conclusion is mandatory before signing.";
                case "digitalSignature" -> "Digital signature is mandatory.";
                case "reason" -> "Specify the reason for the addendum.";
                case "addendumContent" -> "Addendum content is mandatory.";
                default -> "This field is mandatory or invalid.";
            });
        }
        return errors;
    }
}
