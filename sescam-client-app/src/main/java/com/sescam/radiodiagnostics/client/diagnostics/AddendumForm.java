package com.sescam.radiodiagnostics.client.diagnostics;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Form backing and local validation for issuing post-signature report addendums (6a of UC-PHY-04).
 */
public class AddendumForm {

    static final int MAX_CONTENT_LENGTH = 5000;

    private String reportId;
    private String reason;
    private String addendumContent;
    private String digitalSignature;
    private Long expectedVersion;

    public Map<String, String> validate() {
        Map<String, String> errors = new LinkedHashMap<>();
        if (isBlank(reportId)) {
            errors.put("reportId", "Report ID is required.");
        }
        if (isBlank(reason)) {
            errors.put("reason", "Specify the reason for the addendum.");
        }
        if (isBlank(addendumContent)) {
            errors.put("addendumContent", "Addendum content is mandatory.");
        } else if (addendumContent.trim().length() > MAX_CONTENT_LENGTH) {
            errors.put("addendumContent", "Use at most " + MAX_CONTENT_LENGTH + " characters.");
        }
        if (isBlank(digitalSignature)) {
            errors.put("digitalSignature", "Digital signature is mandatory for the addendum.");
        }
        return errors;
    }

    public DiagnosticReportApiClient.AddendumPayload toPayload() {
        return new DiagnosticReportApiClient.AddendumPayload(
                trimToEmpty(reason),
                trimToEmpty(addendumContent),
                trimToEmpty(digitalSignature),
                expectedVersion
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAddendumContent() {
        return addendumContent;
    }

    public void setAddendumContent(String addendumContent) {
        this.addendumContent = addendumContent;
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
