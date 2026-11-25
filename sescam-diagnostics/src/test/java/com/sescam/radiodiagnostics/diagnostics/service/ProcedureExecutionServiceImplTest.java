package com.sescam.radiodiagnostics.diagnostics.service;

import com.sescam.radiodiagnostics.common.diagnostics.ClinicalIncidentDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionAlreadyRecordedException;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionLogDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionRecordDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionStatus;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentDTO;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentSeverity;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentType;
import com.sescam.radiodiagnostics.common.diagnostics.InvalidExecutionDataException;
import com.sescam.radiodiagnostics.common.diagnostics.ProcedureEventPublisher;
import com.sescam.radiodiagnostics.common.diagnostics.ProcedureExecutionEvent;
import com.sescam.radiodiagnostics.common.diagnostics.ProcedureNotFoundException;
import com.sescam.radiodiagnostics.common.diagnostics.ScheduledProcedureDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ScheduledProcedureGateway;
import com.sescam.radiodiagnostics.common.diagnostics.UnauthorizedExecutionAccessException;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.diagnostics.internal.ExecutionRules;
import com.sescam.radiodiagnostics.diagnostics.internal.InMemoryScheduledProcedureGateway;
import com.sescam.radiodiagnostics.diagnostics.repository.ExecutionRepository;
import com.sescam.radiodiagnostics.diagnostics.repository.InMemoryExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ProcedureExecutionServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");
    private static final Instant START = Instant.parse("2026-11-24T09:05:00Z");
    private static final Instant END = Instant.parse("2026-11-24T09:35:00Z");
    private static final IncidentDTO REACTION = new IncidentDTO(IncidentType.ADVERSE_CONTRAST_REACTION,
            IncidentSeverity.SEVERE, "Anaphylactoid reaction after contrast", "Adrenaline 0.5 mg IM",
            Instant.parse("2026-11-24T09:20:00Z"));

    private final AccessControlService accessControl = (actorId, role) -> false;
    private ExecutionRepository repository;
    private ScheduledProcedureGateway gateway;
    private AuditService auditService;
    private ProcedureEventPublisher eventPublisher;
    private ProcedureExecutionServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryExecutionRepository();
        gateway = new InMemoryScheduledProcedureGateway();
        auditService = mock(AuditService.class);
        eventPublisher = mock(ProcedureEventPublisher.class);
        service = new ProcedureExecutionServiceImpl(repository, gateway, accessControl, auditService, eventPublisher,
                new ExecutionRules(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /** Completed contrast CT: the main success scenario. */
    private static ExecutionLogDTO ctLog(boolean aborted, String reason, List<IncidentDTO> incidents) {
        return new ExecutionLogDTO("ORD-DEMO-CT", START, END, "CT-ROOM-1 / Somatom", "IOP-2026-117", 90.0,
                520.0, 14.2, null, true, aborted, reason, incidents, "tech-3", "RADIOLOGY_TECHNICIAN");
    }

    private List<String> auditActions() {
        ArgumentCaptor<AuditEventDTO> captor = ArgumentCaptor.forClass(AuditEventDTO.class);
        verify(auditService, atLeastOnce()).recordEvent(captor.capture());
        return captor.getAllValues().stream().map(AuditEventDTO::action).toList();
    }

    @Test
    @DisplayName("Step 1: lists booked procedures without an execution record, oldest first")
    void listsProceduresAwaitingExecution() {
        assertThat(service.listProceduresAwaitingExecution("dr-1", "PHYSICIAN"))
                .extracting(ScheduledProcedureDTO::orderId)
                .containsExactly("ORD-DEMO-XR", "ORD-DEMO-CT", "ORD-DEMO-MR", "ORD-DEMO-NM");

        service.recordExecution(ctLog(false, null, List.of()));

        assertThat(service.listProceduresAwaitingExecution("dr-1", "PHYSICIAN"))
                .extracting(ScheduledProcedureDTO::orderId).doesNotContain("ORD-DEMO-CT");
        assertThat(auditActions()).containsExactly(
                "EXECUTION_PENDING_LIST_VIEWED", "EXECUTION_RECORDED", "EXECUTION_PENDING_LIST_VIEWED");
    }

    @Test
    @DisplayName("The pending list is clinical data: if its access cannot be audited, nothing is returned (#188)")
    void pendingListNotReturnedWithoutAudit() {
        doThrow(new IllegalStateException("audit store down")).when(auditService).recordEvent(any());

        assertThatThrownBy(() -> service.listProceduresAwaitingExecution("dr-1", "PHYSICIAN"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Main flow: execution metrics stored as EXECUTED_PENDING_REPORT, audited and published")
    void recordsExecution() {
        ExecutionRecordDTO record = service.recordExecution(ctLog(false, null, List.of()));

        assertThat(record.status()).isEqualTo(ExecutionStatus.EXECUTED_PENDING_REPORT);
        assertThat(record.orderId()).isEqualTo("ORD-DEMO-CT");
        assertThat(record.patientId()).isEqualTo("PAT-DEMO-2");
        assertThat(record.startedAt()).isEqualTo(START);
        assertThat(record.endedAt()).isEqualTo(END);
        assertThat(record.roomEquipmentId()).isEqualTo("CT-ROOM-1 / Somatom");
        assertThat(record.contrastBatch()).isEqualTo("IOP-2026-117");
        assertThat(record.contrastVolumeMl()).isEqualTo(90.0);
        assertThat(record.doseDlp()).isEqualTo(520.0);
        assertThat(record.doseCtdiVol()).isEqualTo(14.2);
        assertThat(record.informedConsentVerified()).isTrue();
        assertThat(record.recordedBy()).isEqualTo("tech-3");
        assertThat(record.recordedAt()).isEqualTo(NOW);
        assertThat(record.incidents()).isEmpty();
        assertThat(repository.findByOrderId("ORD-DEMO-CT")).isPresent();
        assertThat(auditActions()).containsExactly("EXECUTION_RECORDED");

        ArgumentCaptor<ProcedureExecutionEvent> event = ArgumentCaptor.forClass(ProcedureExecutionEvent.class);
        verify(eventPublisher).publishExecutionRecorded(event.capture());
        assertThat(event.getValue().status()).isEqualTo(ExecutionStatus.EXECUTED_PENDING_REPORT);
        assertThat(event.getValue().requiresRescheduling()).isFalse();
    }

    @Test
    @DisplayName("2a: incidents are stored with severity, linked to the patient and flagged for audit")
    void recordsIncidents() {
        IncidentDTO noTime = new IncidentDTO(IncidentType.EQUIPMENT_MALFUNCTION, IncidentSeverity.MILD,
                " Injector pressure alarm ", "  ", null);

        ExecutionRecordDTO record = service.recordExecution(ctLog(false, null, List.of(REACTION, noTime)));

        assertThat(record.status()).isEqualTo(ExecutionStatus.EXECUTED_PENDING_REPORT);
        assertThat(record.incidents()).hasSize(2).allSatisfy(i -> {
            assertThat(i.patientId()).isEqualTo("PAT-DEMO-2");
            assertThat(i.executionId()).isEqualTo(record.executionId());
            assertThat(i.procedureAborted()).isFalse();
        });
        ClinicalIncidentDTO first = record.incidents().get(0);
        assertThat(first.severity()).isEqualTo(IncidentSeverity.SEVERE);
        assertThat(first.treatmentGiven()).isEqualTo("Adrenaline 0.5 mg IM");
        ClinicalIncidentDTO second = record.incidents().get(1);
        assertThat(second.description()).isEqualTo("Injector pressure alarm");
        assertThat(second.treatmentGiven()).isNull();
        assertThat(second.occurredAt()).isEqualTo(END);
        assertThat(auditActions()).containsExactly(
                "EXECUTION_RECORDED", "CLINICAL_INCIDENT_LOGGED", "CLINICAL_INCIDENT_LOGGED");
    }

    @Test
    @DisplayName("2b: aborted procedure stored as INCOMPLETE_INCIDENT and admissions told to reschedule")
    void abortsProcedure() {
        ExecutionLogDTO aborted = new ExecutionLogDTO("ORD-DEMO-CT", START, Instant.parse("2026-11-24T09:21:00Z"),
                "CT-ROOM-1", null, null, null, null, null, true, true, "  Severe contrast reaction  ",
                List.of(REACTION), "dr-1", "PHYSICIAN");

        ExecutionRecordDTO record = service.recordExecution(aborted);

        assertThat(record.status()).isEqualTo(ExecutionStatus.INCOMPLETE_INCIDENT);
        assertThat(record.abortReason()).isEqualTo("Severe contrast reaction");
        assertThat(record.incidents()).singleElement().satisfies(i -> assertThat(i.procedureAborted()).isTrue());
        assertThat(auditActions()).containsExactly("PROCEDURE_ABORTED", "CLINICAL_INCIDENT_LOGGED");

        ArgumentCaptor<ProcedureExecutionEvent> event = ArgumentCaptor.forClass(ProcedureExecutionEvent.class);
        verify(eventPublisher).publishExecutionRecorded(event.capture());
        assertThat(event.getValue().requiresRescheduling()).isTrue();
        assertThat(event.getValue().abortReason()).isEqualTo("Severe contrast reaction");
        assertThat(event.getValue().patientId()).isEqualTo("PAT-DEMO-2");
    }

    @Test
    @DisplayName("2b: an abort without reason or incident is rejected and nothing is stored")
    void abortNeedsReasonAndIncident() {
        ExecutionLogDTO bad = new ExecutionLogDTO("ORD-DEMO-XR", START, END, "XR-1", null, null, null, null, null,
                false, true, null, List.of(), "dr-1", "PHYSICIAN");

        assertThatThrownBy(() -> service.recordExecution(bad))
                .isInstanceOfSatisfying(InvalidExecutionDataException.class,
                        ex -> assertThat(ex.getInvalidFields()).containsExactly("abortReason", "incidents"));
        assertThat(repository.count()).isZero();
        assertThat(auditActions()).containsExactly("EXECUTION_REJECTED");
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Consent must be verified and CT doses captured before an execution is stored")
    void rejectsMissingSafetyData() {
        ExecutionLogDTO noConsentNoDose = new ExecutionLogDTO("ORD-DEMO-CT", START, END, "CT-1", "LOT", 80.0,
                null, null, null, false, false, null, null, "dr-1", "PHYSICIAN");

        assertThatThrownBy(() -> service.recordExecution(noConsentNoDose))
                .isInstanceOfSatisfying(InvalidExecutionDataException.class, ex ->
                        assertThat(ex.getInvalidFields()).containsExactly("informedConsentVerified", "doseDlp", "doseCtdiVol"));
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("Only physicians, radiologists and technicians; a blank actor is refused too")
    void deniesOtherActors() {
        assertThatThrownBy(() -> service.recordExecution(new ExecutionLogDTO("ORD-DEMO-XR", START, END, "XR-1",
                null, null, null, null, null, false, false, null, null, "adm-1", "ADMINISTRATIVE_STAFF")))
                .isInstanceOf(UnauthorizedExecutionAccessException.class);
        assertThatThrownBy(() -> service.listProceduresAwaitingExecution(" ", "PHYSICIAN"))
                .isInstanceOf(UnauthorizedExecutionAccessException.class);

        assertThat(repository.count()).isZero();
        assertThat(auditActions()).containsExactly("EXECUTION_ACCESS_DENIED", "EXECUTION_ACCESS_DENIED");
    }

    @Test
    @DisplayName("Unknown, unbooked or missing order is rejected")
    void rejectsUnknownProcedure() {
        assertThatThrownBy(() -> service.recordExecution(new ExecutionLogDTO("ORD-NOPE", START, END, "R", null, null,
                null, null, null, true, false, null, null, "dr-1", "PHYSICIAN")))
                .isInstanceOf(ProcedureNotFoundException.class);
        assertThatThrownBy(() -> service.recordExecution(new ExecutionLogDTO(" ", START, END, "R", null, null,
                null, null, null, true, false, null, null, "dr-1", "PHYSICIAN")))
                .isInstanceOfSatisfying(InvalidExecutionDataException.class,
                        ex -> assertThat(ex.getInvalidFields()).containsExactly("orderId"));
        assertThat(auditActions()).containsExactly("EXECUTION_REJECTED", "EXECUTION_REJECTED");
    }

    @Test
    @DisplayName("A procedure can only be recorded once; the refusal is audited")
    void rejectsSecondRecord() {
        service.recordExecution(ctLog(false, null, List.of()));

        assertThatThrownBy(() -> service.recordExecution(ctLog(false, null, List.of())))
                .isInstanceOf(ExecutionAlreadyRecordedException.class);
        verify(eventPublisher).publishExecutionRecorded(any());
        assertThat(auditActions()).containsExactly("EXECUTION_RECORDED", "EXECUTION_REJECTED");
    }

    @Test
    @DisplayName("Two requests at the same time: the second gets 409, keeps the first and is compensated (#188)")
    void concurrentSecondRecord() {
        // Both requests pass existsByOrderId before either is stored
        InMemoryExecutionRepository racy = new InMemoryExecutionRepository() {
            @Override
            public boolean existsByOrderId(String orderId) {
                return false;
            }
        };
        ProcedureExecutionServiceImpl racyService = new ProcedureExecutionServiceImpl(racy, gateway, accessControl,
                auditService, eventPublisher, new ExecutionRules(), Clock.fixed(NOW, ZoneOffset.UTC));
        ExecutionRecordDTO first = racyService.recordExecution(ctLog(false, null, List.of()));

        assertThatThrownBy(() -> racyService.recordExecution(ctLog(false, null, List.of())))
                .isInstanceOf(ExecutionAlreadyRecordedException.class);

        assertThat(racy.count()).isEqualTo(1);
        assertThat(racy.findByOrderId("ORD-DEMO-CT")).get()
                .satisfies(e -> assertThat(e.getId()).isEqualTo(first.executionId()));
        verify(eventPublisher).publishExecutionRecorded(any());
        assertThat(auditActions()).containsExactly(
                "EXECUTION_RECORDED", "EXECUTION_RECORDED", "EXECUTION_RECORD_FAILED");
    }

    @Test
    @DisplayName("A failing audit store does not hide a refusal, and an execution it cannot audit is not stored")
    void auditFailures() {
        doThrow(new IllegalStateException("audit store down")).when(auditService).recordEvent(any());

        assertThatThrownBy(() -> service.recordExecution(new ExecutionLogDTO("ORD-DEMO-XR", START, END, "XR-1",
                null, null, null, null, null, false, false, null, null, "", "PHYSICIAN")))
                .isInstanceOf(UnauthorizedExecutionAccessException.class);
        assertThatThrownBy(() -> service.recordExecution(ctLog(false, null, List.of())))
                .isInstanceOf(IllegalStateException.class);
        assertThat(repository.count()).isZero();
        verify(eventPublisher, never()).publishExecutionRecorded(any());
    }

    @Test
    @DisplayName("An incident that cannot be audited leaves nothing stored, and a retry succeeds without a 409")
    void incidentAuditFailureThenRetry() {
        doNothing().doThrow(new IllegalStateException("audit store down")).doNothing()
                .when(auditService).recordEvent(any());

        assertThatThrownBy(() -> service.recordExecution(ctLog(false, null, List.of(REACTION))))
                .isInstanceOf(IllegalStateException.class);
        assertThat(repository.count()).isZero();
        verify(eventPublisher, never()).publishExecutionRecorded(any());
        // The execution entry already recorded is compensated (#188)
        assertThat(auditActions()).containsExactly(
                "EXECUTION_RECORDED", "CLINICAL_INCIDENT_LOGGED", "EXECUTION_RECORD_FAILED");

        ExecutionRecordDTO retry = service.recordExecution(ctLog(false, null, List.of(REACTION)));

        assertThat(retry.status()).isEqualTo(ExecutionStatus.EXECUTED_PENDING_REPORT);
        assertThat(repository.count()).isEqualTo(1);
        verify(eventPublisher).publishExecutionRecorded(any());
    }

    @Test
    @DisplayName("Constructor and request reject nulls")
    void nullChecks() {
        assertThatNullPointerException().isThrownBy(() -> service.recordExecution(null));
        assertThatNullPointerException().isThrownBy(() -> new ProcedureExecutionServiceImpl(null, gateway,
                accessControl, auditService, eventPublisher));
        assertThatNullPointerException().isThrownBy(() -> new ProcedureExecutionServiceImpl(repository, null,
                accessControl, auditService, eventPublisher));
        assertThatNullPointerException().isThrownBy(() -> new ProcedureExecutionServiceImpl(repository, gateway,
                null, auditService, eventPublisher));
        assertThatNullPointerException().isThrownBy(() -> new ProcedureExecutionServiceImpl(repository, gateway,
                accessControl, null, eventPublisher));
        assertThatNullPointerException().isThrownBy(() -> new ProcedureExecutionServiceImpl(repository, gateway,
                accessControl, auditService, null));
    }
}
