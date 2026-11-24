package com.sescam.radiodiagnostics.diagnostics.domain;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionStatus;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentSeverity;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentType;
import com.sescam.radiodiagnostics.common.diagnostics.ProcedureExecutionEvent;
import com.sescam.radiodiagnostics.common.diagnostics.ScheduledProcedureDTO;
import com.sescam.radiodiagnostics.diagnostics.internal.InMemoryScheduledProcedureGateway;
import com.sescam.radiodiagnostics.diagnostics.internal.NoOpProcedureEventPublisher;
import com.sescam.radiodiagnostics.diagnostics.repository.InMemoryExecutionRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ExecutionDomainTest {

    private static final Instant T = Instant.parse("2026-11-24T09:00:00Z");

    private static ProcedureExecution execution(List<ClinicalIncident> incidents) {
        return new ProcedureExecution("EXE-1", "ORD-1", "PAT-1", "Chest X-ray", ModalityType.X_RAY, T, T, "XR-1",
                null, null, null, null, null, false, ExecutionStatus.EXECUTED_PENDING_REPORT, null, incidents,
                "dr-1", T);
    }

    @Test
    void executionCopiesItsIncidentsAndMapsToDto() {
        ClinicalIncident incident = new ClinicalIncident("INC-1", "EXE-1", "PAT-1", IncidentType.OTHER_OPERATIONAL,
                IncidentSeverity.MILD, "Late start", null, T, false);
        ProcedureExecution execution = execution(List.of(incident));

        assertThat(execution.getIncidents()).containsExactly(incident);
        assertThat(execution.toDTO().incidents()).singleElement()
                .satisfies(i -> assertThat(i.incidentId()).isEqualTo("INC-1"));
        assertThat(execution(null).getIncidents()).isEmpty();
        assertThat(execution.getRecordedAt()).isEqualTo(T);
        assertThat(incident.getId()).isEqualTo("INC-1");
    }

    @Test
    void mandatoryFieldsAreRequired() {
        assertThatNullPointerException().isThrownBy(() -> new ProcedureExecution("E", "O", null, null, null, T, T,
                "R", null, null, null, null, null, false, ExecutionStatus.EXECUTED_PENDING_REPORT, null, null, "dr", T));
        assertThatNullPointerException().isThrownBy(() -> new ClinicalIncident("I", "E", "P", null,
                IncidentSeverity.MILD, "d", null, T, false));
    }

    @Test
    void repositoryKeepsOneRecordPerOrder() {
        InMemoryExecutionRepository repository = new InMemoryExecutionRepository();
        repository.save(execution(List.of()));

        assertThat(repository.existsByOrderId("ORD-1")).isTrue();
        assertThat(repository.existsByOrderId(null)).isFalse();
        assertThat(repository.findByOrderId("ORD-2")).isEmpty();
        assertThat(repository.findByOrderId(null)).isEmpty();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void standInsBehaveAsDocumented() {
        InMemoryScheduledProcedureGateway gateway = new InMemoryScheduledProcedureGateway(List.of(
                new ScheduledProcedureDTO("B", "P", "E", "n", ModalityType.CT, null, "R", false, false),
                new ScheduledProcedureDTO("A", "P", "E", "n", ModalityType.CT, T, "R", false, false)));

        assertThat(gateway.listBookedProcedures()).extracting(ScheduledProcedureDTO::orderId).containsExactly("A", "B");
        assertThat(gateway.findByOrderId(null)).isEmpty();
        assertThat(gateway.findByOrderId("A")).isPresent();
        assertThatCode(() -> new NoOpProcedureEventPublisher().publishExecutionRecorded(
                new ProcedureExecutionEvent("E", "O", "P", ExecutionStatus.EXECUTED_PENDING_REPORT, null, T)))
                .doesNotThrowAnyException();
    }
}
