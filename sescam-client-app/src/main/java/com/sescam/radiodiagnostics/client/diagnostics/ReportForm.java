package com.sescam.radiodiagnostics.client.diagnostics;

import com.sescam.radiodiagnostics.common.diagnostics.ReportDTO;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Form backing and local validation for authoring/signing diagnostic reports (UC-PHY-04).
 */
public class ReportForm {

    static final int MAX_TEXT_LENGTH = 5000;

    private String executionId;
    private String findings;
    private String conclusion;
    private boolean criticalAlert;
    private boolean followUpRecommended;
    private String digitalSignature;
    private Long expectedVersion;

    public static ReportForm fromReport(ReportDTO report) {
        ReportForm form = new ReportForm();
        form.setExecutionId(report.executionId());
        form.setFindings(report.findings());
        form.setConclusion(report.conclusion());
        form.setCriticalAlert(report.criticalAlert());
        form.setFollowUpRecommended(report.followUpRecommended());
        form.setExpectedVersion(report.version());
        return form;
    }

    public Map<String, String> validateDraft() {
        Map<String, String> errors = new LinkedHashMap<>();
        if (isBlank(executionId)) {
            errors.put("executionId", "Execution ID is required.");
        }
        checkLength(errors, "findings", findings);
        checkLength(errors, "conclusion", conclusion);
        return errors;
    }

    public Map<String, String> validateFinalize() {
        Map<String, String> errors = validateDraft();
        if (isBlank(findings)) {
            errors.put("findings", "Clinical findings are mandatory before signing.");
        }
        if (isBlank(conclusion)) {
            errors.put("conclusion", "Diagnostic conclusion is mandatory before signing.");
        }
        if (isBlank(digitalSignature)) {
            errors.put("digitalSignature", "Digital signature is mandatory to finalize the report.");
        }
        return errors;
    }

    public DiagnosticReportApiClient.DraftPayload toDraftPayload() {
        return new DiagnosticReportApiClient.DraftPayload(
                trimToEmpty(executionId),
                trimToEmpty(findings),
                trimToEmpty(conclusion),
                criticalAlert,
                followUpRecommended,
                expectedVersion
        );
    }

    public DiagnosticReportApiClient.FinalizePayload toFinalizePayload() {
        return new DiagnosticReportApiClient.FinalizePayload(
                trimToEmpty(executionId),
                trimToEmpty(findings),
                trimToEmpty(conclusion),
                criticalAlert,
                followUpRecommended,
                trimToEmpty(digitalSignature),
                expectedVersion
        );
    }

    private static void checkLength(Map<String, String> errors, String field, String value) {
        if (value != null && value.trim().length() > MAX_TEXT_LENGTH) {
            errors.put(field, "Use at most " + MAX_TEXT_LENGTH + " characters.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getFindings() {
        return findings;
    }

    public void setFindings(String findings) {
        this.findings = findings;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public boolean isCriticalAlert() {
        return criticalAlert;
    }

    public void setCriticalAlert(boolean criticalAlert) {
        this.criticalAlert = criticalAlert;
    }

    public boolean isFollowUpRecommended() {
        return followUpRecommended;
    }

    public void setFollowUpRecommended(boolean followUpRecommended) {
        this.followUpRecommended = followUpRecommended;
    }

    public String getDigitalSignature() {
        return digitalSignature;
    }

    public void setDigitalSignature(String digitalSignature) {
        this.digitalSignature = digitalSignature;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
