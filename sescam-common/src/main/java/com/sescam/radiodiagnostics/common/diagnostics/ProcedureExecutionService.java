package com.sescam.radiodiagnostics.common.diagnostics;

import java.util.List;

/**
 * Public service contract for C7 (sescam-diagnostics) realizing UC-PHY-05: Log Procedure Execution Details and
 * Clinical/Operational Incidents. Later use cases of C7 (PHY-04, PAT-02) add their own methods or interfaces.
 */
public interface ProcedureExecutionService {

    /**
     * Step 1: booked procedures that have no execution record yet, oldest scheduled first.
     *
     * @throws UnauthorizedExecutionAccessException if the actor may not log executions
     */
    List<ScheduledProcedureDTO> listProceduresAwaitingExecution(String actorId, String actorRole);

    /**
     * Steps 2-5, 2a and 2b. Records the execution of a booked procedure with its incidents.
     *
     * <p>Without {@code aborted} the execution is stored as {@link ExecutionStatus#EXECUTED_PENDING_REPORT}. With
     * {@code aborted} (2b) it is stored as {@link ExecutionStatus#INCOMPLETE_INCIDENT}, it must carry a reason and at
     * least one incident, and admissions is told to reschedule through {@link ProcedureEventPublisher}. Every incident
     * (2a) is linked to the patient and audited.
     *
     * @throws UnauthorizedExecutionAccessException if the actor may not log executions
     * @throws ProcedureNotFoundException           if the order is not a booked procedure
     * @throws ExecutionAlreadyRecordedException    if the procedure already has an execution record
     * @throws InvalidExecutionDataException        if mandatory data is missing or inconsistent
     */
    ExecutionRecordDTO recordExecution(ExecutionLogDTO request);
}
