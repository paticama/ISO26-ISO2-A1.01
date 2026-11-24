package com.sescam.radiodiagnostics.common.diagnostics;

/**
 * Outbound port of C7 for the hand-off execution -> orders / admissions (ADR-014).
 *
 * <p>C4 (orders) updates the order status and C5 (admissions) reschedules aborted procedures (2b). Until they
 * subscribe, the server wires a no-op implementation.
 */
public interface ProcedureEventPublisher {

    void publishExecutionRecorded(ProcedureExecutionEvent event);
}
