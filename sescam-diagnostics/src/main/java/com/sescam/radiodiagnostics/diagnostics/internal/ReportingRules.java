package com.sescam.radiodiagnostics.diagnostics.internal;

import com.sescam.radiodiagnostics.common.diagnostics.FinalizeReportRequestDTO;
import com.sescam.radiodiagnostics.common.diagnostics.IssueReportAddendumRequestDTO;
import com.sescam.radiodiagnostics.common.diagnostics.SaveReportDraftRequestDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Business validation rules for diagnostic reports (UC-PHY-04).
 */
public class ReportingRules {

    public List<String> invalidDraftFields(SaveReportDraftRequestDTO request) {
        List<String> invalid = new ArrayList<>();
        if (isBlank(request.executionId())) {
            invalid.add("executionId");
        }
        return invalid;
    }

    public List<String> invalidFinalizeFields(FinalizeReportRequestDTO request) {
        List<String> invalid = new ArrayList<>();
        if (isBlank(request.executionId())) {
            invalid.add("executionId");
        }
        if (isBlank(request.findings())) {
            invalid.add("findings");
        }
        if (isBlank(request.conclusion())) {
            invalid.add("conclusion");
        }
        if (isBlank(request.digitalSignature())) {
            invalid.add("digitalSignature");
        }
        return invalid;
    }

    public List<String> invalidAddendumFields(IssueReportAddendumRequestDTO request) {
        List<String> invalid = new ArrayList<>();
        if (isBlank(request.reportId())) {
            invalid.add("reportId");
        }
        if (isBlank(request.reason())) {
            invalid.add("reason");
        }
        if (isBlank(request.addendumContent())) {
            invalid.add("addendumContent");
        }
        if (isBlank(request.digitalSignature())) {
            invalid.add("digitalSignature");
        }
        return invalid;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
