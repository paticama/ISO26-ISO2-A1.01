package com.sescam.radiodiagnostics.diagnostics.service;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
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
import com.sescam.radiodiagnostics.diagnostics.internal.InMemoryPacsGateway;
import com.sescam.radiodiagnostics.diagnostics.repository.ExecutionRepository;
import com.sescam.radiodiagnostics.diagnostics.repository.InMemoryExecutionRepository;
import com.sescam.radiodiagnostics.diagnostics.repository.InMemoryReportRepository;
import com.sescam.radiodiagnostics.diagnostics.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DiagnosticReportingServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-26T12:00:00Z");
    private static final Instant EXEC_START = Instant.parse("2026-11-26T09:00:00Z");
    private static final Instant EXEC_END = Instant.parse("2026-11-26T09:25:00Z");

    private final AccessControlService accessControl = (actorId, role) -> false;
    private ExecutionRepository executionRepository;
    private ReportRepository reportRepository;
    private PacsGateway pacsGateway;
    private FhirDiagnosticReportGateway fhirGateway;
    private ReportEventPublisher eventPublisher;
    private AuditService auditService;
    private DiagnosticReportingServiceImpl service;

    @BeforeEach
    void setUp() {
        executionRepository = new InMemoryExecutionRepository();
        reportRepository = new InMemoryReportRepository();
        pacsGateway = new InMemoryPacsGateway();
        fhirGateway = mock(FhirDiagnosticReportGateway.class);
        when(fhirGateway.exportDiagnosticReport(any())).thenReturn(EhrReportSyncStatus.SYNCED);
        eventPublisher = mock(ReportEventPublisher.class);
        auditService = mock(AuditService.class);

        executionRepository.save(new ProcedureExecution(
                "EXE-1", "ORD-1", "PAT-1", "Abdominal CT with contrast", ModalityType.CT,
                EXEC_START, EXEC_END, "CT-ROOM-1", "IOP-1", 90.0, 480.0, 13.5, null,
                true, ExecutionStatus.EXECUTED_PENDING_REPORT, null, List.of(), "tech-1", EXEC_END));
        executionRepository.save(new ProcedureExecution(
                "EXE-ABORT", "ORD-2", "PAT-2", "Knee MRI", ModalityType.MRI,
                EXEC_START, EXEC_END, "MR-ROOM-1", null, null, null, null, null,
                true, ExecutionStatus.INCOMPLETE_INCIDENT, "Claustrophobia", List.of(), "tech-1", EXEC_END));

        service = new DiagnosticReportingServiceImpl(reportRepository, executionRepository, pacsGateway,
                fhirGateway, eventPublisher, accessControl, auditService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private List<String> auditActions() {
        ArgumentCaptor<AuditEventDTO> captor = ArgumentCaptor.forClass(AuditEventDTO.class);
        verify(auditService, atLeastOnce()).recordEvent(captor.capture());
        return captor.getAllValues().stream().map(AuditEventDTO::action).toList();
    }

    @Test
    @DisplayName("Step 1: worklist lists only executed procedures awaiting/in reporting with current status and audits access")
    void listsProceduresPendingReport() {
        List<ReportableProcedureDTO> initial = service.listProceduresPendingReport("dr-1", "PHYSICIAN");
        assertThat(initial).singleElement().satisfies(p -> {
            assertThat(p.executionId()).isEqualTo("EXE-1");
            assertThat(p.reportId()).isNull();
            assertThat(p.reportStatus()).isNull();
        });

        ReportDTO draft = service.saveDraft(new SaveReportDraftRequestDTO(
                "EXE-1", "Mild steatosis", "", false, false, null, "dr-1", "RADIOLOGIST"));

        List<ReportableProcedureDTO> afterDraft = service.listProceduresPendingReport("dr-1", "RADIOLOGIST");
        assertThat(afterDraft).singleElement().satisfies(p -> {
            assertThat(p.reportId()).isEqualTo(draft.reportId());
            assertThat(p.reportStatus()).isEqualTo(ReportStatus.DRAFT);
        });
        assertThat(auditActions()).containsExactly(
                "REPORT_WORKLIST_VIEWED", "REPORT_DRAFT_SAVED", "REPORT_WORKLIST_VIEWED");
    }

    @Test
    @DisplayName("NFR-11: worklist refuses to return clinical data when audit recording fails")
    void worklistFailsIfAuditFails() {
        doThrow(new IllegalStateException("audit unavailable")).when(auditService).recordEvent(any());

        assertThatThrownBy(() -> service.listProceduresPendingReport("dr-1", "PHYSICIAN"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Step 2: retrieves DICOM study metadata & WADO viewer token and audits study access")
    void retrievesDicomStudy() {
        DicomStudyDTO study = service.getDicomStudy("EXE-1", "dr-1", "PHYSICIAN");

        assertThat(study.executionId()).isEqualTo("EXE-1");
        assertThat(study.patientId()).isEqualTo("PAT-1");
        assertThat(study.wadoViewerToken()).isEqualTo("wado-exe-1");
        assertThat(study.numberOfSeries()).isEqualTo(4);
        assertThat(auditActions()).containsExactly("DICOM_STUDY_ACCESSED");
    }

    @Test
    @DisplayName("Alt 2a: PACS connection failure logs audit incident and throws PacsConnectionException")
    void handlesPacsFailure() {
        PacsGateway failingPacs = exec -> {
            throw new PacsConnectionException("PACS WADO-RS timeout");
        };
        DiagnosticReportingServiceImpl failingService = new DiagnosticReportingServiceImpl(
                reportRepository, executionRepository, failingPacs, fhirGateway, eventPublisher,
                accessControl, auditService, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> failingService.getDicomStudy("EXE-1", "dr-1", "PHYSICIAN"))
                .isInstanceOf(PacsConnectionException.class)
                .hasMessageContaining("timeout");
        assertThat(auditActions()).containsExactly("PACS_CONNECTION_FAILED");
    }

    @Test
    @DisplayName("Alt 5a: saving a draft persists DRAFT without triggering FHIR sync or patient notifications")
    void saveDraftDoesNotSyncOrNotify() {
        ReportDTO created = service.saveDraft(new SaveReportDraftRequestDTO(
                "EXE-1", "Liver parenchyma homogeneous", "Normal liver", false, false, null, "dr-1", "PHYSICIAN"));

        assertThat(created.status()).isEqualTo(ReportStatus.DRAFT);
        assertThat(created.ehrSyncStatus()).isEqualTo(EhrReportSyncStatus.NOT_SYNCED);
        assertThat(created.version()).isEqualTo(1L);
        assertThat(created.digitalSignature()).isNull();

        ReportDTO updated = service.saveDraft(new SaveReportDraftRequestDTO(
                "EXE-1", "Small hepatic cyst", "Benign cyst", false, true, 1L, "dr-1", "PHYSICIAN"));

        assertThat(updated.reportId()).isEqualTo(created.reportId());
        assertThat(updated.version()).isEqualTo(2L);
        assertThat(updated.followUpRecommended()).isTrue();
        verifyNoInteractions(fhirGateway);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Main flow steps 6-8: finalize & sign exports FHIR DiagnosticReport and emits ReportAvailabilityEvent")
    void finalizeAndSignExportsAndNotifies() {
        service.saveDraft(new SaveReportDraftRequestDTO(
                "EXE-1", "Draft findings", "Draft conclusion", false, false, null, "dr-1", "PHYSICIAN"));

        ReportDTO finalized = service.finalizeAndSign(new FinalizeReportRequestDTO(
                "EXE-1", "Appendicitis with fat stranding", "Acute appendicitis", true, false,
                "SIG-SHA256-DR1", 1L, "dr-1", "PHYSICIAN"));

        assertThat(finalized.status()).isEqualTo(ReportStatus.FINALIZED);
        assertThat(finalized.digitalSignature()).isEqualTo("SIG-SHA256-DR1");
        assertThat(finalized.signedAt()).isEqualTo(NOW);
        assertThat(finalized.ehrSyncStatus()).isEqualTo(EhrReportSyncStatus.SYNCED);
        assertThat(finalized.ehrSyncedAt()).isEqualTo(NOW);
        assertThat(finalized.criticalAlert()).isTrue();

        verify(fhirGateway).exportDiagnosticReport(any());
        ArgumentCaptor<ReportAvailabilityEvent> eventCaptor = ArgumentCaptor.forClass(ReportAvailabilityEvent.class);
        verify(eventPublisher).publishReportAvailable(eventCaptor.capture());
        assertThat(eventCaptor.getValue().criticalAlert()).isTrue();
        assertThat(eventCaptor.getValue().addendum()).isFalse();
        assertThat(eventCaptor.getValue().patientId()).isEqualTo("PAT-1");
        assertThat(auditActions()).containsExactly("REPORT_DRAFT_SAVED", "REPORT_FINALIZED");
    }

    @Test
    @DisplayName("Finalize still persists signed report and marks FAILED_SYNC when EHR FHIR export fails")
    void finalizeRecordsFailedEhrSyncVisibly() {
        when(fhirGateway.exportDiagnosticReport(any())).thenThrow(new IllegalStateException("FHIR endpoint down"));

        ReportDTO finalized = service.finalizeAndSign(new FinalizeReportRequestDTO(
                "EXE-1", "Findings", "Conclusion", false, false, "SIG-1", null, "dr-1", "RADIOLOGIST"));

        assertThat(finalized.status()).isEqualTo(ReportStatus.FINALIZED);
        assertThat(finalized.ehrSyncStatus()).isEqualTo(EhrReportSyncStatus.FAILED_SYNC);
        assertThat(finalized.ehrSyncedAt()).isNull();
        verify(eventPublisher).publishReportAvailable(any());
        assertThat(auditActions()).containsExactly("REPORT_FINALIZED", "REPORT_EHR_SYNC_FAILED");
    }

    @Test
    @DisplayName("Alt 6a: addendum preserves original signature, sets AMENDED, re-exports FHIR and emits addendum event")
    void issueAddendumPreservesOriginalSignature() {
        ReportDTO finalized = service.finalizeAndSign(new FinalizeReportRequestDTO(
                "EXE-1", "Findings", "Conclusion", false, false, "SIG-ORIGINAL", null, "dr-1", "PHYSICIAN"));

        ReportDTO amended = service.issueAddendum(new IssueReportAddendumRequestDTO(
                finalized.reportId(), "Additional clinical history", "Incidental renal cyst noted",
                "SIG-ADDENDUM", finalized.version(), "dr-2", "RADIOLOGIST"));

        assertThat(amended.status()).isEqualTo(ReportStatus.AMENDED);
        assertThat(amended.digitalSignature()).isEqualTo("SIG-ORIGINAL");
        assertThat(amended.signedAt()).isEqualTo(NOW);
        assertThat(amended.addendums()).singleElement().satisfies(a -> {
            assertThat(a.reason()).isEqualTo("Additional clinical history");
            assertThat(a.content()).isEqualTo("Incidental renal cyst noted");
            assertThat(a.authorPhysicianId()).isEqualTo("dr-2");
            assertThat(a.digitalSignature()).isEqualTo("SIG-ADDENDUM");
        });

        ArgumentCaptor<ReportAvailabilityEvent> events = ArgumentCaptor.forClass(ReportAvailabilityEvent.class);
        verify(eventPublisher, atLeastOnce()).publishReportAvailable(events.capture());
        assertThat(events.getAllValues()).extracting(ReportAvailabilityEvent::addendum).containsExactly(false, true);
        assertThat(auditActions()).containsExactly("REPORT_FINALIZED", "REPORT_ADDENDUM_ISSUED");
    }

    @Test
    @DisplayName("Consult methods return reports by id or executionId and enforce mandatory validation")
    void consultAndValidationErrors() {
        assertThat(service.findReportByExecutionId("EXE-1", "dr-1", "PHYSICIAN")).isEmpty();

        ReportDTO draft = service.saveDraft(new SaveReportDraftRequestDTO(
                "EXE-1", "Findings", "", false, false, null, "dr-1", "PHYSICIAN"));

        assertThat(service.getReportById(draft.reportId(), "dr-1", "PHYSICIAN").reportId()).isEqualTo(draft.reportId());
        assertThat(service.findReportByExecutionId("EXE-1", "dr-1", "PHYSICIAN")).isPresent();

        assertThatThrownBy(() -> service.getReportById("MISSING", "dr-1", "PHYSICIAN"))
                .isInstanceOf(ReportNotFoundException.class);
        assertThatThrownBy(() -> service.getReportById(" ", "dr-1", "PHYSICIAN"))
                .isInstanceOf(InvalidReportDataException.class);
        assertThatThrownBy(() -> service.findReportByExecutionId(" ", "dr-1", "PHYSICIAN"))
                .isInstanceOf(InvalidReportDataException.class);
        assertThatThrownBy(() -> service.getDicomStudy(" ", "dr-1", "PHYSICIAN"))
                .isInstanceOf(InvalidReportDataException.class);
        assertThatThrownBy(() -> service.getDicomStudy("EXE-NOPE", "dr-1", "PHYSICIAN"))
                .isInstanceOf(ReportNotFoundException.class);
        assertThatThrownBy(() -> service.getDicomStudy("EXE-ABORT", "dr-1", "PHYSICIAN"))
                .isInstanceOf(ReportStateConflictException.class);

        assertThatThrownBy(() -> service.saveDraft(new SaveReportDraftRequestDTO(
                " ", "F", "C", false, false, null, "dr-1", "PHYSICIAN")))
                .isInstanceOf(InvalidReportDataException.class)
                .satisfies(ex -> assertThat(((InvalidReportDataException) ex).getInvalidFields()).containsExactly("executionId"));

        assertThatThrownBy(() -> service.finalizeAndSign(new FinalizeReportRequestDTO(
                " ", " ", " ", false, false, " ", null, "dr-1", "PHYSICIAN")))
                .isInstanceOf(InvalidReportDataException.class)
                .satisfies(ex -> assertThat(((InvalidReportDataException) ex).getInvalidFields())
                        .containsExactly("executionId", "findings", "conclusion", "digitalSignature"));

        assertThatThrownBy(() -> service.issueAddendum(new IssueReportAddendumRequestDTO(
                " ", " ", " ", " ", null, "dr-1", "PHYSICIAN")))
                .isInstanceOf(InvalidReportDataException.class)
                .satisfies(ex -> assertThat(((InvalidReportDataException) ex).getInvalidFields())
                        .containsExactly("reportId", "reason", "addendumContent", "digitalSignature"));
    }

    @Test
    @DisplayName("State and version conflicts prevent invalid transitions and stale overwrites")
    void stateAndVersionConflicts() {
        ReportDTO draft = service.saveDraft(new SaveReportDraftRequestDTO(
                "EXE-1", "F", "C", false, false, null, "dr-1", "PHYSICIAN"));

        assertThatThrownBy(() -> service.saveDraft(new SaveReportDraftRequestDTO(
                "EXE-1", "F2", "C2", false, false, 99L, "dr-1", "PHYSICIAN")))
                .isInstanceOf(ReportStateConflictException.class);

        assertThatThrownBy(() -> service.issueAddendum(new IssueReportAddendumRequestDTO(
                draft.reportId(), "Reason", "Content", "SIG", null, "dr-1", "PHYSICIAN")))
                .isInstanceOf(ReportStateConflictException.class);

        ReportDTO finalized = service.finalizeAndSign(new FinalizeReportRequestDTO(
                "EXE-1", "F", "C", false, false, "SIG", 1L, "dr-1", "PHYSICIAN"));

        assertThatThrownBy(() -> service.saveDraft(new SaveReportDraftRequestDTO(
                "EXE-1", "F2", "C2", false, false, null, "dr-1", "PHYSICIAN")))
                .isInstanceOf(ReportStateConflictException.class);
        assertThatThrownBy(() -> service.finalizeAndSign(new FinalizeReportRequestDTO(
                "EXE-1", "F2", "C2", false, false, "SIG2", null, "dr-1", "PHYSICIAN")))
                .isInstanceOf(ReportStateConflictException.class);
        assertThatThrownBy(() -> service.issueAddendum(new IssueReportAddendumRequestDTO(
                finalized.reportId(), "Reason", "Content", "SIG2", 99L, "dr-1", "PHYSICIAN")))
                .isInstanceOf(ReportStateConflictException.class);
        assertThatThrownBy(() -> service.issueAddendum(new IssueReportAddendumRequestDTO(
                "REP-MISSING", "Reason", "Content", "SIG2", null, "dr-1", "PHYSICIAN")))
                .isInstanceOf(ReportNotFoundException.class);
    }

    @Test
    @DisplayName("Compensating audit entry is recorded if persistence fails after REPORT_FINALIZED audit")
    void compensatingAuditWhenRepositoryFails() {
        ReportRepository failingRepo = new InMemoryReportRepository() {
            @Override
            public DiagnosticReport save(DiagnosticReport report) {
                throw new IllegalStateException("storage error");
            }
        };
        DiagnosticReportingServiceImpl svc = new DiagnosticReportingServiceImpl(
                failingRepo, executionRepository, pacsGateway, fhirGateway, eventPublisher,
                accessControl, auditService, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> svc.finalizeAndSign(new FinalizeReportRequestDTO(
                "EXE-1", "F", "C", false, false, "SIG", null, "dr-1", "PHYSICIAN")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(auditActions()).containsExactly("REPORT_FINALIZED", "REPORT_FINALIZE_FAILED");
        verify(fhirGateway, never()).exportDiagnosticReport(any());
        verify(eventPublisher, never()).publishReportAvailable(any());
    }

    @Test
    @DisplayName("RBAC: technicians, admins, patients, and blank actors are denied access (403)")
    void enforcesPhysicianAndRadiologistAccessControl() {
        assertThatThrownBy(() -> service.listProceduresPendingReport("tech-1", "RADIOLOGY_TECHNICIAN"))
                .isInstanceOf(UnauthorizedReportingAccessException.class);
        assertThatThrownBy(() -> service.getDicomStudy("EXE-1", "adm-1", "ADMINISTRATIVE_STAFF"))
                .isInstanceOf(UnauthorizedReportingAccessException.class);
        assertThatThrownBy(() -> service.saveDraft(new SaveReportDraftRequestDTO(
                "EXE-1", "F", "C", false, false, null, " ", "PHYSICIAN")))
                .isInstanceOf(UnauthorizedReportingAccessException.class);
        assertThatThrownBy(() -> service.finalizeAndSign(new FinalizeReportRequestDTO(
                "EXE-1", "F", "C", false, false, "SIG", null, "pat-1", null)))
                .isInstanceOf(UnauthorizedReportingAccessException.class);
        assertThat(auditActions()).containsOnly("REPORT_ACCESS_DENIED");
    }
}
