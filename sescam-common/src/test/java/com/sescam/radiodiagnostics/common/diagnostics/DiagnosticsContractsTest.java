package com.sescam.radiodiagnostics.common.diagnostics;

import com.sescam.radiodiagnostics.common.security.AccessControlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiagnosticsContractsTest {

    private static final Instant T = Instant.parse("2026-11-26T09:00:00Z");

    @Test
    @DisplayName("canLogProcedureExecution allows physicians, radiologists and technicians only")
    void canLogDefault() {
        AccessControlService ac = (actorId, role) -> false;

        assertThat(ac.canLogProcedureExecution("u", "PHYSICIAN")).isTrue();
        assertThat(ac.canLogProcedureExecution("u", " radiologist ")).isTrue();
        assertThat(ac.canLogProcedureExecution("u", "RADIOLOGY_TECHNICIAN")).isTrue();
        assertThat(ac.canLogProcedureExecution("u", "ADMINISTRATOR")).isFalse();
        assertThat(ac.canLogProcedureExecution("u", "PATIENT")).isFalse();
        assertThat(ac.canLogProcedureExecution("u", null)).isFalse();
    }

    @Test
    @DisplayName("ExecutionLogDTO reads null incidents as none and copies the list")
    void executionLogIncidents() {
        ExecutionLogDTO none = new ExecutionLogDTO("ORD-1", T, T, "CT-1", null, null, null, null, null,
                true, false, null, null, "dr", "PHYSICIAN");
        assertThat(none.incidents()).isEmpty();

        List<IncidentDTO> source = new ArrayList<>(List.of(new IncidentDTO(IncidentType.EQUIPMENT_MALFUNCTION,
                IncidentSeverity.MILD, "Injector alarm", null, T)));
        ExecutionLogDTO one = new ExecutionLogDTO("ORD-1", T, T, "CT-1", null, null, null, null, null,
                true, false, null, source, "dr", "PHYSICIAN");
        source.clear();
        assertThat(one.incidents()).hasSize(1);
        assertThatThrownBy(() -> one.incidents().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("ExecutionRecordDTO reads null incidents as none")
    void executionRecordIncidents() {
        ExecutionRecordDTO record = new ExecutionRecordDTO("EXE-1", "ORD-1", "PAT-1", "CT", null, T, T, "CT-1",
                null, null, null, null, null, true, ExecutionStatus.EXECUTED_PENDING_REPORT, null, null, "dr", T);
        assertThat(record.incidents()).isEmpty();
    }

    @Test
    @DisplayName("Only an INCOMPLETE_INCIDENT execution asks admissions to reschedule (2b)")
    void eventRescheduling() {
        assertThat(new ProcedureExecutionEvent("E", "O", "P", ExecutionStatus.INCOMPLETE_INCIDENT, "reaction", T)
                .requiresRescheduling()).isTrue();
        assertThat(new ProcedureExecutionEvent("E", "O", "P", ExecutionStatus.EXECUTED_PENDING_REPORT, null, T)
                .requiresRescheduling()).isFalse();
    }

    @Test
    @DisplayName("Exceptions keep their message; invalid fields are an immutable copy")
    void exceptions() {
        List<String> fields = new ArrayList<>(List.of("doseDlp"));
        InvalidExecutionDataException invalid = new InvalidExecutionDataException("Invalid", fields);
        fields.clear();

        assertThat(invalid.getInvalidFields()).containsExactly("doseDlp");
        assertThatThrownBy(() -> invalid.getInvalidFields().add("x")).isInstanceOf(UnsupportedOperationException.class);
        assertThat(new UnauthorizedExecutionAccessException("a").getMessage()).isEqualTo("a");
        assertThat(new ProcedureNotFoundException("b").getMessage()).isEqualTo("b");
        assertThat(new ExecutionAlreadyRecordedException("c").getMessage()).isEqualTo("c");
    }
}
