package com.sescam.radiodiagnostics.common.diagnostics;

import java.util.List;
import java.util.Optional;

/**
 * Public service contract for C7 (sescam-diagnostics) realizing UC-PHY-04:
 * Author, Edit, and Review Diagnostic Reports and Medical Images.
 */
public interface DiagnosticReportingService {

    /** Step 1: executed procedures (EXECUTED_PENDING_REPORT) with current report status if any. */
    List<ReportableProcedureDTO> listProceduresPendingReport(String actorId, String actorRole);

    /** Step 2 (+2a): retrieve DICOM study metadata and WADO viewer access token for an execution. */
    DicomStudyDTO getDicomStudy(String executionId, String actorId, String actorRole);

    /** Consult an existing diagnostic report by id. */
    ReportDTO getReportById(String reportId, String actorId, String actorRole);

    /** Consult an existing diagnostic report for a procedure execution if one has been started. */
    Optional<ReportDTO> findReportByExecutionId(String executionId, String actorId, String actorRole);

    /** Step 5a: save or update a draft report without triggering EHR sync or patient notification. */
    ReportDTO saveDraft(SaveReportDraftRequestDTO request);

    /** Steps 6-8: validate, digitally sign, persist, export HL7 FHIR DiagnosticReport, and emit availability event. */
    ReportDTO finalizeAndSign(FinalizeReportRequestDTO request);

    /** Step 6a: append a signed addendum to a finalized/amended report preserving the original signature. */
    ReportDTO issueAddendum(IssueReportAddendumRequestDTO request);
}
