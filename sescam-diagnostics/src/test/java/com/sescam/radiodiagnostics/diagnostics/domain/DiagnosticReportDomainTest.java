package com.sescam.radiodiagnostics.diagnostics.domain;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.diagnostics.DicomStudyDTO;
import com.sescam.radiodiagnostics.common.diagnostics.EhrReportSyncStatus;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionStatus;
import com.sescam.radiodiagnostics.common.diagnostics.ReportAvailabilityEvent;
import com.sescam.radiodiagnostics.common.diagnostics.ReportDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ReportStateConflictException;
import com.sescam.radiodiagnostics.common.diagnostics.ReportStatus;
import com.sescam.radiodiagnostics.diagnostics.internal.InMemoryPacsGateway;
import com.sescam.radiodiagnostics.diagnostics.internal.NoOpFhirDiagnosticReportGateway;
import com.sescam.radiodiagnostics.diagnostics.internal.NoOpReportEventPublisher;
import com.sescam.radiodiagnostics.diagnostics.repository.InMemoryExecutionRepository;
import com.sescam.radiodiagnostics.diagnostics.repository.InMemoryReportRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiagnosticReportDomainTest {

    private static final Instant T0 = Instant.parse("2026-11-26T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-11-26T10:15:00Z");
    private static final Instant T2 = Instant.parse("2026-11-26T10:30:00Z");

    private static ProcedureExecution execution(String id, String orderId, ExecutionStatus status, ModalityType modality) {
        return new ProcedureExecution(id, orderId, "PAT-1", "Chest Exam", modality, T0, T1, "ROOM-1",
                null, null, null, null, null, true, status,
                status == ExecutionStatus.INCOMPLETE_INCIDENT ? "Aborted" : null, List.of(), "tech-1", T1);
    }

    @Test
    void lifecycleDraftFinalizeAddendumAndEhrSync() {
        ProcedureExecution exec = execution("EXE-1", "ORD-1", ExecutionStatus.EXECUTED_PENDING_REPORT, ModalityType.CT);
        DiagnosticReport report = DiagnosticReport.createDraft("REP-1", exec, "1.2.3.4",
                "Initial findings", "Pending correlation", false, false, "dr-1", T0);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.DRAFT);
        assertThat(report.getVersion()).isEqualTo(1L);
        assertThat(report.getEhrSyncStatus()).isEqualTo(EhrReportSyncStatus.NOT_SYNCED);
        assertThat(report.getAddendums()).isEmpty();
        assertThat(exec.getExamTypeName()).isEqualTo("Chest Exam");
        assertThat(exec.getModality()).isEqualTo(ModalityType.CT);

        assertThatThrownBy(() -> report.addAddendum(
                new ReportAddendum("ADD-1", "Typo", "Correction", "dr-1", "SIG-ADD", T1), T1))
                .isInstanceOf(ReportStateConflictException.class);

        report.updateDraft("Updated findings", "Normal study", true, true, "dr-2", T1);
        assertThat(report.getVersion()).isEqualTo(2L);
        assertThat(report.isCriticalAlert()).isTrue();

        report.finalizeAndSign("Final findings", "Pulmonary embolism", true, true, "dr-2", "SIG-ORIG", T2);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.FINALIZED);
        assertThat(report.getVersion()).isEqualTo(3L);

        assertThatThrownBy(() -> report.updateDraft("Edit", "Edit", false, false, "dr-2", T2))
                .isInstanceOf(ReportStateConflictException.class);
        assertThatThrownBy(() -> report.finalizeAndSign("Edit", "Edit", false, false, "dr-2", "SIG-2", T2))
                .isInstanceOf(ReportStateConflictException.class);

        ReportAddendum addendum = new ReportAddendum("ADD-1", "Laterality check", "Right lower lobe", "dr-2", "SIG-ADD", T2);
        report.addAddendum(addendum, T2);
        report.markEhrSync(EhrReportSyncStatus.SYNCED, T2);

        ReportDTO dto = report.toDTO();
        assertThat(dto.status()).isEqualTo(ReportStatus.AMENDED);
        assertThat(dto.digitalSignature()).isEqualTo("SIG-ORIG");
        assertThat(dto.signedAt()).isEqualTo(T2);
        assertThat(dto.ehrSyncStatus()).isEqualTo(EhrReportSyncStatus.SYNCED);
        assertThat(dto.addendums()).singleElement().satisfies(a -> {
            assertThat(a.addendumId()).isEqualTo("ADD-1");
            assertThat(a.digitalSignature()).isEqualTo("SIG-ADD");
        });
    }

    @Test
    void repositoriesAndStandInsBehaveAsDocumented() {
        InMemoryExecutionRepository execRepo = new InMemoryExecutionRepository();
        ProcedureExecution ok1 = execution("EXE-1", "ORD-1", ExecutionStatus.EXECUTED_PENDING_REPORT, ModalityType.CT);
        ProcedureExecution aborted = execution("EXE-2", "ORD-2", ExecutionStatus.INCOMPLETE_INCIDENT, ModalityType.MRI);
        execRepo.save(ok1);
        execRepo.save(aborted);

        assertThat(execRepo.findById("EXE-1")).contains(ok1);
        assertThat(execRepo.findById("NOPE")).isEmpty();
        assertThat(execRepo.findById(null)).isEmpty();
        assertThat(execRepo.findAllExecutedPendingReport()).containsExactly(ok1);

        InMemoryReportRepository reportRepo = new InMemoryReportRepository();
        DiagnosticReport r1 = DiagnosticReport.createDraft("REP-1", ok1, "1.2.3", "F", "C", false, false, "dr-1", T0);
        reportRepo.save(r1);
        assertThat(reportRepo.count()).isEqualTo(1);
        assertThat(reportRepo.findById("REP-1")).contains(r1);
        assertThat(reportRepo.findById(null)).isEmpty();
        assertThat(reportRepo.findByExecutionId("EXE-1")).contains(r1);
        assertThat(reportRepo.findByExecutionId("MISSING")).isEmpty();
        assertThat(reportRepo.findByExecutionId(null)).isEmpty();

        DiagnosticReport duplicate = DiagnosticReport.createDraft("REP-2", ok1, "1.2.3", "F", "C", false, false, "dr-1", T0);
        assertThatThrownBy(() -> reportRepo.save(duplicate)).isInstanceOf(ReportStateConflictException.class);

        InMemoryPacsGateway pacs = new InMemoryPacsGateway();
        DicomStudyDTO ctStudy = pacs.fetchStudyForExecution(ok1.toDTO());
        assertThat(ctStudy.numberOfSeries()).isEqualTo(4);
        assertThat(ctStudy.numberOfInstances()).isEqualTo(180);
        assertThat(ctStudy.wadoViewerToken()).isEqualTo("wado-exe-1");

        DicomStudyDTO nmStudy = pacs.fetchStudyForExecution(
                execution("EXE-3", "ORD-3", ExecutionStatus.EXECUTED_PENDING_REPORT, ModalityType.NUCLEAR_MEDICINE).toDTO());
        assertThat(nmStudy.numberOfSeries()).isEqualTo(3);
        assertThat(nmStudy.numberOfInstances()).isEqualTo(45);

        DicomStudyDTO xrStudy = pacs.fetchStudyForExecution(
                execution("EXE-4", "ORD-4", ExecutionStatus.EXECUTED_PENDING_REPORT, ModalityType.X_RAY).toDTO());
        assertThat(xrStudy.numberOfSeries()).isEqualTo(2);
        assertThat(xrStudy.numberOfInstances()).isEqualTo(4);

        DicomStudyDTO nullModStudy = pacs.fetchStudyForExecution(
                execution("EXE-5", "ORD-5", ExecutionStatus.EXECUTED_PENDING_REPORT, null).toDTO());
        assertThat(nullModStudy.numberOfSeries()).isEqualTo(1);
        assertThat(nullModStudy.numberOfInstances()).isEqualTo(1);

        assertThat(new NoOpFhirDiagnosticReportGateway().exportDiagnosticReport(r1.toDTO()))
                .isEqualTo(EhrReportSyncStatus.SYNCED);
        assertThatCode(() -> new NoOpReportEventPublisher().publishReportAvailable(
                new ReportAvailabilityEvent("REP-1", "EXE-1", "ORD-1", "PAT-1", false, false, T0)))
                .doesNotThrowAnyException();
    }
}
