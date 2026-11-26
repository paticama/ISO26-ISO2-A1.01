package com.sescam.radiodiagnostics.diagnostics.service;

import com.sescam.radiodiagnostics.common.diagnostics.DiagnosticReportingService;
import com.sescam.radiodiagnostics.common.diagnostics.DicomStudyDTO;
import com.sescam.radiodiagnostics.common.diagnostics.EhrReportSyncStatus;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionStatus;
import com.sescam.radiodiagnostics.common.diagnostics.FhirDiagnosticReportGateway;
import com.sescam.radiodiagnostics.common.diagnostics.FinalizeReportRequestDTO;
import com.sescam.radiodiagnostics.common.diagnostics.InvalidReportDataException;
import com.sescam.radiodiagnostics.common.diagnostics.IssueReportAddendumRequestDTO;
import com.sescam.radiodiagnostics.common.diagnostics.PacsConnectionException;
import com.sescam.radiodiagnostics.common.diagnostics.PacsGateway;
import com.sescam.radiodiagnostics.common.diagnostics.ReportAvailabilityEvent;
import com.sescam.radiodiagnostics.common.diagnostics.ReportDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ReportEventPublisher;
import com.sescam.radiodiagnostics.common.diagnostics.ReportNotFoundException;
import com.sescam.radiodiagnostics.common.diagnostics.ReportStateConflictException;
import com.sescam.radiodiagnostics.common.diagnostics.ReportStatus;
import com.sescam.radiodiagnostics.common.diagnostics.ReportableProcedureDTO;
import com.sescam.radiodiagnostics.common.diagnostics.SaveReportDraftRequestDTO;
import com.sescam.radiodiagnostics.common.diagnostics.UnauthorizedReportingAccessException;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.diagnostics.domain.DiagnosticReport;
import com.sescam.radiodiagnostics.diagnostics.domain.ProcedureExecution;
import com.sescam.radiodiagnostics.diagnostics.domain.ReportAddendum;
import com.sescam.radiodiagnostics.diagnostics.internal.InMemoryPacsGateway;
import com.sescam.radiodiagnostics.diagnostics.internal.ReportingRules;
import com.sescam.radiodiagnostics.diagnostics.repository.ExecutionRepository;
import com.sescam.radiodiagnostics.diagnostics.repository.ReportRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Realization of UC-PHY-04: Author, Edit, and Review Diagnostic Reports and Medical Images.
 */
public class DiagnosticReportingServiceImpl implements DiagnosticReportingService {

    private final ReportRepository reportRepository;
    private final ExecutionRepository executionRepository;
    private final PacsGateway pacsGateway;
    private final FhirDiagnosticReportGateway fhirGateway;
    private final ReportEventPublisher eventPublisher;
    private final AccessControlService accessControlService;
    private final AuditService auditService;
    private final ReportingRules rules;
    private final Clock clock;

    public DiagnosticReportingServiceImpl(ReportRepository reportRepository,
                                          ExecutionRepository executionRepository,
                                          PacsGateway pacsGateway,
                                          FhirDiagnosticReportGateway fhirGateway,
                                          ReportEventPublisher eventPublisher,
                                          AccessControlService accessControlService,
                                          AuditService auditService) {
        this(reportRepository, executionRepository, pacsGateway, fhirGateway, eventPublisher,
                accessControlService, auditService, new ReportingRules(), Clock.systemUTC());
    }

    public DiagnosticReportingServiceImpl(ReportRepository reportRepository,
                                          ExecutionRepository executionRepository,
                                          PacsGateway pacsGateway,
                                          FhirDiagnosticReportGateway fhirGateway,
                                          ReportEventPublisher eventPublisher,
                                          AccessControlService accessControlService,
                                          AuditService auditService,
                                          Clock clock) {
        this(reportRepository, executionRepository, pacsGateway, fhirGateway, eventPublisher,
                accessControlService, auditService, new ReportingRules(), clock);
    }

    public DiagnosticReportingServiceImpl(ReportRepository reportRepository,
                                          ExecutionRepository executionRepository,
                                          PacsGateway pacsGateway,
                                          FhirDiagnosticReportGateway fhirGateway,
                                          ReportEventPublisher eventPublisher,
                                          AccessControlService accessControlService,
                                          AuditService auditService,
                                          ReportingRules rules,
                                          Clock clock) {
        this.reportRepository = Objects.requireNonNull(reportRepository, "reportRepository must not be null");
        this.executionRepository = Objects.requireNonNull(executionRepository, "executionRepository must not be null");
        this.pacsGateway = Objects.requireNonNull(pacsGateway, "pacsGateway must not be null");
        this.fhirGateway = Objects.requireNonNull(fhirGateway, "fhirGateway must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.accessControlService = Objects.requireNonNull(accessControlService, "accessControlService must not be null");
        this.auditService = Objects.requireNonNull(auditService, "auditService must not be null");
        this.rules = Objects.requireNonNull(rules, "rules must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public List<ReportableProcedureDTO> listProceduresPendingReport(String actorId, String actorRole) {
        checkAccess(actorId, actorRole, null);
        List<ProcedureExecution> executed = executionRepository.findAllExecutedPendingReport();
        List<ReportableProcedureDTO> worklist = executed.stream()
                .map(exec -> {
                    Optional<DiagnosticReport> report = reportRepository.findByExecutionId(exec.getId());
                    return new ReportableProcedureDTO(
                            exec.getId(),
                            exec.getOrderId(),
                            exec.getPatientId(),
                            exec.getExamTypeName(),
                            exec.getModality(),
                            exec.getRecordedAt(),
                            report.map(DiagnosticReport::getId).orElse(null),
                            report.map(DiagnosticReport::getStatus).orElse(null)
                    );
                })
                .toList();
        audit("REPORT_WORKLIST_VIEWED", actorId, null,
                worklist.size() + " executed procedures in reporting worklist");
        return worklist;
    }

    @Override
    public DicomStudyDTO getDicomStudy(String executionId, String actorId, String actorRole) {
        checkAccess(actorId, actorRole, executionId);
        if (isBlank(executionId)) {
            throw new InvalidReportDataException("The executionId is mandatory.", List.of("executionId"));
        }
        ProcedureExecution execution = requireReportableExecution(executionId.trim(), actorId);
        try {
            DicomStudyDTO study = pacsGateway.fetchStudyForExecution(execution.toDTO());
            audit("DICOM_STUDY_ACCESSED", actorId, execution.getId(),
                    "Accessed study " + study.studyInstanceUid() + " for patient " + execution.getPatientId());
            return study;
        } catch (PacsConnectionException ex) {
            auditRefusal("PACS_CONNECTION_FAILED", actorId, execution.getId(), ex.getMessage());
            throw ex;
        }
    }

    @Override
    public ReportDTO getReportById(String reportId, String actorId, String actorRole) {
        checkAccess(actorId, actorRole, reportId);
        if (isBlank(reportId)) {
            throw new InvalidReportDataException("The reportId is mandatory.", List.of("reportId"));
        }
        DiagnosticReport report = reportRepository.findById(reportId.trim())
                .orElseThrow(() -> new ReportNotFoundException("Diagnostic report '" + reportId + "' not found."));
        audit("REPORT_VIEWED", actorId, report.getId(),
                "Viewed report for patient " + report.getPatientId() + " (" + report.getStatus() + ")");
        return report.toDTO();
    }

    @Override
    public Optional<ReportDTO> findReportByExecutionId(String executionId, String actorId, String actorRole) {
        checkAccess(actorId, actorRole, executionId);
        if (isBlank(executionId)) {
            throw new InvalidReportDataException("The executionId is mandatory.", List.of("executionId"));
        }
        Optional<DiagnosticReport> report = reportRepository.findByExecutionId(executionId.trim());
        report.ifPresent(r -> audit("REPORT_VIEWED", actorId, r.getId(),
                "Viewed report for execution " + executionId.trim() + " (" + r.getStatus() + ")"));
        return report.map(DiagnosticReport::toDTO);
    }

    @Override
    public ReportDTO saveDraft(SaveReportDraftRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");
        checkAccess(request.actorId(), request.actorRole(), request.executionId());

        List<String> invalid = rules.invalidDraftFields(request);
        if (!invalid.isEmpty()) {
            auditRefusal("REPORT_REJECTED", request.actorId(), request.executionId(), "Invalid draft: " + invalid);
            throw new InvalidReportDataException("The draft report request is invalid: " + String.join(", ", invalid) + ".",
                    invalid);
        }

        String executionId = request.executionId().trim();
        ProcedureExecution execution = requireReportableExecution(executionId, request.actorId());
        Instant now = clock.instant();

        DiagnosticReport report = reportRepository.findByExecutionId(executionId).orElse(null);
        if (report == null) {
            if (request.expectedVersion() != null && request.expectedVersion() != 0L) {
                throw new ReportStateConflictException("Report version conflict for execution '" + executionId + "'.");
            }
            report = DiagnosticReport.createDraft(
                    "REP-" + shortId(),
                    execution,
                    InMemoryPacsGateway.studyUidFor(executionId),
                    request.findings(),
                    request.conclusion(),
                    request.criticalAlert(),
                    request.followUpRecommended(),
                    request.actorId(),
                    now
            );
        } else {
            checkExpectedVersion(report, request.expectedVersion(), request.actorId());
            report.updateDraft(request.findings(), request.conclusion(), request.criticalAlert(),
                    request.followUpRecommended(), request.actorId(), now);
        }

        DiagnosticReport saved = persistWithAudit(report, "REPORT_DRAFT_SAVED", "REPORT_DRAFT_SAVE_FAILED",
                request.actorId(),
                "Draft saved for execution " + executionId + ", patient " + report.getPatientId()
                        + ", v" + report.getVersion());
        return saved.toDTO();
    }

    @Override
    public ReportDTO finalizeAndSign(FinalizeReportRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");
        checkAccess(request.actorId(), request.actorRole(), request.executionId());

        List<String> invalid = rules.invalidFinalizeFields(request);
        if (!invalid.isEmpty()) {
            auditRefusal("REPORT_REJECTED", request.actorId(), request.executionId(), "Invalid finalize: " + invalid);
            throw new InvalidReportDataException(
                    "Finalizing a diagnostic report requires findings, conclusion, and digital signature.", invalid);
        }

        String executionId = request.executionId().trim();
        ProcedureExecution execution = requireReportableExecution(executionId, request.actorId());
        Instant now = clock.instant();

        DiagnosticReport report = reportRepository.findByExecutionId(executionId).orElse(null);
        if (report == null) {
            if (request.expectedVersion() != null && request.expectedVersion() != 0L) {
                throw new ReportStateConflictException("Report version conflict for execution '" + executionId + "'.");
            }
            report = DiagnosticReport.createDraft(
                    "REP-" + shortId(),
                    execution,
                    InMemoryPacsGateway.studyUidFor(executionId),
                    request.findings(),
                    request.conclusion(),
                    request.criticalAlert(),
                    request.followUpRecommended(),
                    request.actorId(),
                    now
            );
        } else {
            if (report.getStatus() != ReportStatus.DRAFT) {
                auditRefusal("REPORT_REJECTED", request.actorId(), report.getId(),
                        "Already " + report.getStatus());
                throw new ReportStateConflictException(
                        "Report '" + report.getId() + "' is already " + report.getStatus() + ".");
            }
            checkExpectedVersion(report, request.expectedVersion(), request.actorId());
        }

        report.finalizeAndSign(request.findings(), request.conclusion(), request.criticalAlert(),
                request.followUpRecommended(), request.actorId(), request.digitalSignature().trim(), now);

        DiagnosticReport saved = persistWithAudit(report, "REPORT_FINALIZED", "REPORT_FINALIZE_FAILED",
                request.actorId(),
                "Finalized report for patient " + report.getPatientId()
                        + (report.isCriticalAlert() ? " [CRITICAL ALERT]" : ""));

        exportToEhrAndUpdateSync(saved, request.actorId());
        eventPublisher.publishReportAvailable(new ReportAvailabilityEvent(
                saved.getId(), saved.getExecutionId(), execution.getOrderId(),
                saved.getPatientId(), saved.isCriticalAlert(), false, now));

        return saved.toDTO();
    }

    @Override
    public ReportDTO issueAddendum(IssueReportAddendumRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");
        checkAccess(request.actorId(), request.actorRole(), request.reportId());

        List<String> invalid = rules.invalidAddendumFields(request);
        if (!invalid.isEmpty()) {
            auditRefusal("REPORT_ADDENDUM_REJECTED", request.actorId(), request.reportId(),
                    "Invalid addendum: " + invalid);
            throw new InvalidReportDataException(
                    "Issuing an addendum requires reportId, reason, content, and digital signature.", invalid);
        }

        String reportId = request.reportId().trim();
        DiagnosticReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Diagnostic report '" + reportId + "' not found."));

        if (report.getStatus() == ReportStatus.DRAFT) {
            auditRefusal("REPORT_ADDENDUM_REJECTED", request.actorId(), reportId, "Report is still DRAFT");
            throw new ReportStateConflictException(
                    "Cannot issue an addendum to draft report '" + reportId + "'; finalize it first.");
        }
        checkExpectedVersion(report, request.expectedVersion(), request.actorId());

        Instant now = clock.instant();
        ReportAddendum addendum = new ReportAddendum(
                "ADD-" + shortId(),
                request.reason().trim(),
                request.addendumContent().trim(),
                request.actorId(),
                request.digitalSignature().trim(),
                now
        );
        report.addAddendum(addendum, now);

        DiagnosticReport saved = persistWithAudit(report, "REPORT_ADDENDUM_ISSUED", "REPORT_ADDENDUM_FAILED",
                request.actorId(),
                "Addendum " + addendum.getId() + " issued for report " + savedReportLabel(report)
                        + " (" + addendumCountLabel(report) + ")");

        exportToEhrAndUpdateSync(saved, request.actorId());
        ReportDTO dto = saved.toDTO();
        eventPublisher.publishReportAvailable(new ReportAvailabilityEvent(
                dto.reportId(), dto.executionId(), dto.orderId(),
                dto.patientId(), dto.criticalAlert(), true, now));

        return dto;
    }

    private ProcedureExecution requireReportableExecution(String executionId, String actorId) {
        ProcedureExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ReportNotFoundException(
                        "Executed procedure '" + executionId + "' not found."));
        if (execution.getStatus() != ExecutionStatus.EXECUTED_PENDING_REPORT) {
            auditRefusal("REPORT_REJECTED", actorId, executionId, "Execution status: " + execution.getStatus());
            throw new ReportStateConflictException(
                    "Procedure execution '" + executionId + "' has status " + execution.getStatus()
                            + " and cannot be reported.");
        }
        return execution;
    }

    private void checkExpectedVersion(DiagnosticReport report, Long expectedVersion, String actorId) {
        if (expectedVersion != null && !expectedVersion.equals(report.getVersion())) {
            auditRefusal("REPORT_VERSION_CONFLICT", actorId, report.getId(),
                    "Expected v" + expectedVersion + " but current is v" + report.getVersion());
            throw new ReportStateConflictException(
                    "Report '" + report.getId() + "' was modified concurrently (expected version "
                            + expectedVersion + ", current " + report.getVersion() + ").");
        }
    }

    private DiagnosticReport persistWithAudit(DiagnosticReport report, String auditAction, String failureAction,
                                              String actorId, String details) {
        boolean audited = false;
        try {
            audit(auditAction, actorId, report.getId(), details);
            audited = true;
            return reportRepository.save(report);
        } catch (RuntimeException ex) {
            if (audited) {
                auditRefusal(failureAction, actorId, report.getId(),
                        "Report operation failed after audit: " + ex.getClass().getSimpleName());
            }
            throw ex;
        }
    }

    private void exportToEhrAndUpdateSync(DiagnosticReport report, String actorId) {
        EhrReportSyncStatus syncStatus;
        try {
            EhrReportSyncStatus result = fhirGateway.exportDiagnosticReport(report.toDTO());
            syncStatus = result == null ? EhrReportSyncStatus.FAILED_SYNC : result;
        } catch (RuntimeException ex) {
            syncStatus = EhrReportSyncStatus.FAILED_SYNC;
        }
        report.markEhrSync(syncStatus, syncStatus == EhrReportSyncStatus.SYNCED ? clock.instant() : null);
        reportRepository.save(report);
        if (syncStatus == EhrReportSyncStatus.FAILED_SYNC) {
            auditRefusal("REPORT_EHR_SYNC_FAILED", actorId, report.getId(),
                    "HL7 FHIR DiagnosticReport export failed for report " + report.getId());
        }
    }

    private void checkAccess(String actorId, String actorRole, String resourceKey) {
        if (isBlank(actorId) || !accessControlService.canAuthorDiagnosticReports(actorId, actorRole)) {
            auditRefusal("REPORT_ACCESS_DENIED", actorId, resourceKey, "Role: " + actorRole);
            throw new UnauthorizedReportingAccessException(
                    "Role '" + actorRole + "' is not authorized to access DICOM studies or author diagnostic reports.");
        }
    }

    private void audit(String action, String actorId, String resourceKey, String details) {
        auditService.recordEvent(new AuditEventDTO(action, isBlank(actorId) ? "UNKNOWN" : actorId, resourceKey,
                null, null, details, clock.instant()));
    }

    private void auditRefusal(String action, String actorId, String resourceKey, String details) {
        try {
            audit(action, actorId, resourceKey, details);
        } catch (RuntimeException ex) {
            // Best effort: preserve original refusal/error
        }
    }

    private static String savedReportLabel(DiagnosticReport report) {
        return report.getId() + " (patient " + report.getPatientId() + ")";
    }

    private static String addendumCountLabel(DiagnosticReport report) {
        return "addendums=" + report.getAddendums().size();
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
