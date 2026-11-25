package com.sescam.radiodiagnostics.diagnostics.service;

import com.sescam.radiodiagnostics.common.diagnostics.ExecutionAlreadyRecordedException;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionLogDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionRecordDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionStatus;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentDTO;
import com.sescam.radiodiagnostics.common.diagnostics.InvalidExecutionDataException;
import com.sescam.radiodiagnostics.common.diagnostics.ProcedureEventPublisher;
import com.sescam.radiodiagnostics.common.diagnostics.ProcedureExecutionEvent;
import com.sescam.radiodiagnostics.common.diagnostics.ProcedureExecutionService;
import com.sescam.radiodiagnostics.common.diagnostics.ProcedureNotFoundException;
import com.sescam.radiodiagnostics.common.diagnostics.ScheduledProcedureDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ScheduledProcedureGateway;
import com.sescam.radiodiagnostics.common.diagnostics.UnauthorizedExecutionAccessException;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.diagnostics.domain.ClinicalIncident;
import com.sescam.radiodiagnostics.diagnostics.domain.ProcedureExecution;
import com.sescam.radiodiagnostics.diagnostics.internal.ExecutionRules;
import com.sescam.radiodiagnostics.diagnostics.repository.ExecutionRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Realization of UC-PHY-05: Log Procedure Execution Details and Clinical/Operational Incidents.
 */
public class ProcedureExecutionServiceImpl implements ProcedureExecutionService {

    private final ExecutionRepository executionRepository;
    private final ScheduledProcedureGateway procedureGateway;
    private final AccessControlService accessControlService;
    private final AuditService auditService;
    private final ProcedureEventPublisher eventPublisher;
    private final ExecutionRules rules;
    private final Clock clock;

    public ProcedureExecutionServiceImpl(ExecutionRepository executionRepository,
                                         ScheduledProcedureGateway procedureGateway,
                                         AccessControlService accessControlService,
                                         AuditService auditService,
                                         ProcedureEventPublisher eventPublisher) {
        this(executionRepository, procedureGateway, accessControlService, auditService, eventPublisher,
                new ExecutionRules(), Clock.systemUTC());
    }

    public ProcedureExecutionServiceImpl(ExecutionRepository executionRepository,
                                         ScheduledProcedureGateway procedureGateway,
                                         AccessControlService accessControlService,
                                         AuditService auditService,
                                         ProcedureEventPublisher eventPublisher,
                                         ExecutionRules rules,
                                         Clock clock) {
        this.executionRepository = Objects.requireNonNull(executionRepository, "executionRepository must not be null");
        this.procedureGateway = Objects.requireNonNull(procedureGateway, "procedureGateway must not be null");
        this.accessControlService = Objects.requireNonNull(accessControlService, "accessControlService must not be null");
        this.auditService = Objects.requireNonNull(auditService, "auditService must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.rules = Objects.requireNonNull(rules, "rules must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public List<ScheduledProcedureDTO> listProceduresAwaitingExecution(String actorId, String actorRole) {
        checkAccess(actorId, actorRole, null);
        List<ScheduledProcedureDTO> pending = procedureGateway.listBookedProcedures().stream()
                .filter(p -> !executionRepository.existsByOrderId(p.orderId()))
                .toList();
        // Clinical-data access (patients, exams, rooms): audited, and nothing is returned if the audit fails (NFR-11).
        audit("EXECUTION_PENDING_LIST_VIEWED", actorId, null, pending.size() + " procedures awaiting execution: "
                + pending.stream().map(ScheduledProcedureDTO::orderId).toList());
        return pending;
    }

    @Override
    public ExecutionRecordDTO recordExecution(ExecutionLogDTO request) {
        Objects.requireNonNull(request, "request must not be null");
        checkAccess(request.actorId(), request.actorRole(), request.orderId());

        if (isBlank(request.orderId())) {
            auditRefusal("EXECUTION_REJECTED", request.actorId(), null, "Missing orderId");
            throw new InvalidExecutionDataException("The procedure (orderId) is mandatory.", List.of("orderId"));
        }
        String orderId = request.orderId().trim();
        ScheduledProcedureDTO procedure = procedureGateway.findByOrderId(orderId).orElse(null);
        if (procedure == null) {
            auditRefusal("EXECUTION_REJECTED", request.actorId(), orderId, "Not a booked procedure");
            throw new ProcedureNotFoundException("Order '" + orderId + "' is not a booked procedure.");
        }
        if (executionRepository.existsByOrderId(orderId)) {
            auditRefusal("EXECUTION_REJECTED", request.actorId(), orderId, "Execution already recorded");
            throw new ExecutionAlreadyRecordedException("The execution of order '" + orderId + "' is already recorded.");
        }

        Instant now = clock.instant();
        List<String> invalid = rules.invalidFields(request, procedure, now);
        if (!invalid.isEmpty()) {
            auditRefusal("EXECUTION_REJECTED", request.actorId(), orderId, "Invalid: " + invalid);
            throw new InvalidExecutionDataException(
                    "The execution log is not valid: " + String.join(", ", invalid) + ".", invalid);
        }

        String executionId = "EXE-" + shortId();
        ExecutionStatus status = request.aborted() ? ExecutionStatus.INCOMPLETE_INCIDENT : ExecutionStatus.EXECUTED_PENDING_REPORT;
        List<ClinicalIncident> incidents = new ArrayList<>();
        for (IncidentDTO i : request.incidents()) {
            incidents.add(new ClinicalIncident("INC-" + shortId(), executionId, procedure.patientId(), i.type(),
                    i.severity(), i.description().trim(), blankToNull(i.treatmentGiven()),
                    i.occurredAt() == null ? request.endedAt() : i.occurredAt(), request.aborted()));
        }

        ProcedureExecution execution = new ProcedureExecution(
                executionId, orderId, procedure.patientId(), procedure.examTypeName(), procedure.modality(),
                request.startedAt(), request.endedAt(), request.roomEquipmentId().trim(),
                blankToNull(request.contrastBatch()), request.contrastVolumeMl(),
                request.doseDlp(), request.doseCtdiVol(), request.radiotracerActivityMBq(),
                request.informedConsentVerified(), status, request.aborted() ? request.abortReason().trim() : null,
                incidents, request.actorId(), now);

        // Audit before saving (NFR-11): if the audit store fails, nothing is stored or published,
        // so the client gets an error and a retry does not hit "already recorded" (409).
        boolean audited = false;
        ProcedureExecution saved;
        try {
            audit(request.aborted() ? "PROCEDURE_ABORTED" : "EXECUTION_RECORDED", request.actorId(), execution.getId(),
                    "Order " + orderId + ", patient " + execution.getPatientId() + ", status " + status
                            + (request.aborted() ? ", reason: " + execution.getAbortReason() : ""));
            audited = true;
            for (ClinicalIncident incident : execution.getIncidents()) {
                audit("CLINICAL_INCIDENT_LOGGED", request.actorId(), incident.getId(),
                        incident.getSeverity() + " " + incident.getType() + " for patient " + incident.getPatientId()
                                + " during order " + orderId);
            }
            // Atomic: a concurrent second request for the same order gets 409 here
            saved = executionRepository.save(execution);
        } catch (RuntimeException ex) {
            if (audited) {
                // Audit entries cannot be changed (NFR-11): record that the audited execution was not stored.
                auditRefusal("EXECUTION_RECORD_FAILED", request.actorId(), execution.getId(),
                        "Execution of order " + orderId + " was not stored: " + ex.getClass().getSimpleName());
            }
            throw ex;
        }
        eventPublisher.publishExecutionRecorded(new ProcedureExecutionEvent(saved.getId(), orderId,
                saved.getPatientId(), status, saved.getAbortReason(), now));

        return saved.toDTO();
    }

    private void checkAccess(String actorId, String actorRole, String resourceKey) {
        if (isBlank(actorId) || !accessControlService.canLogProcedureExecution(actorId, actorRole)) {
            auditRefusal("EXECUTION_ACCESS_DENIED", actorId, resourceKey, "Role: " + actorRole);
            throw new UnauthorizedExecutionAccessException(
                    "Role '" + actorRole + "' may not log procedure executions.");
        }
    }

    /**
     * Audit of an execution or incident about to be stored: if it cannot be recorded, the request fails
     * before anything is stored (NFR-11).
     */
    private void audit(String action, String actorId, String resourceKey, String details) {
        auditService.recordEvent(new AuditEventDTO(action, isBlank(actorId) ? "UNKNOWN" : actorId, resourceKey,
                null, null, details, clock.instant()));
    }

    /**
     * Best-effort audit of a refused request or of an audited execution that was not stored: a failing audit store
     * does not hide the refusal or the original error.
     */
    private void auditRefusal(String action, String actorId, String resourceKey, String details) {
        try {
            audit(action, actorId, resourceKey, details);
        } catch (RuntimeException ex) {
            // best effort: nothing was stored, the caller still gets the refusal
        }
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
