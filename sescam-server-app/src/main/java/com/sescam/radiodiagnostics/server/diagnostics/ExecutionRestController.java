package com.sescam.radiodiagnostics.server.diagnostics;

import com.sescam.radiodiagnostics.common.diagnostics.ExecutionLogDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionRecordDTO;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ProcedureExecutionService;
import com.sescam.radiodiagnostics.common.diagnostics.ScheduledProcedureDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * REST API for UC-PHY-05: Log Procedure Execution Details and Clinical/Operational Incidents.
 */
@RestController
@RequestMapping("/api/v1/executions")
public class ExecutionRestController {

    private final ProcedureExecutionService executionService;

    public ExecutionRestController(ProcedureExecutionService executionService) {
        this.executionService = executionService;
    }

    /** Step 1: booked procedures that still have no execution record. */
    @GetMapping("/pending")
    public ResponseEntity<List<ScheduledProcedureDTO>> pending(
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.ok(executionService.listProceduresAwaitingExecution(actorId, actorRole));
    }

    /** Steps 2-5 with incidents (2a) and abort (2b) in the same request. */
    @PostMapping
    public ResponseEntity<ExecutionRecordDTO> record(
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestBody ExecutionLogPayload payload) {
        // An empty incident entry becomes an incident without data: the service checks the role first and then
        // rejects it as incidents[i].type/severity/description (400, audited), instead of answering here.
        List<IncidentDTO> incidents = payload.incidents() == null ? List.of() : payload.incidents().stream()
                .map(i -> i == null ? new IncidentDTO(null, null, null, null, null) : i)
                .toList();
        ExecutionLogDTO request = new ExecutionLogDTO(payload.orderId(), payload.startedAt(), payload.endedAt(),
                payload.roomEquipmentId(), payload.contrastBatch(), payload.contrastVolumeMl(), payload.doseDlp(),
                payload.doseCtdiVol(), payload.radiotracerActivityMBq(),
                Boolean.TRUE.equals(payload.informedConsentVerified()), Boolean.TRUE.equals(payload.aborted()),
                payload.abortReason(), incidents, actorId, actorRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(executionService.recordExecution(request));
    }

    /** Request body: the actor comes from the headers, never from the body. */
    public record ExecutionLogPayload(
            String orderId,
            Instant startedAt,
            Instant endedAt,
            String roomEquipmentId,
            String contrastBatch,
            Double contrastVolumeMl,
            Double doseDlp,
            Double doseCtdiVol,
            Double radiotracerActivityMBq,
            Boolean informedConsentVerified,
            Boolean aborted,
            String abortReason,
            List<IncidentDTO> incidents
    ) {
    }
}
