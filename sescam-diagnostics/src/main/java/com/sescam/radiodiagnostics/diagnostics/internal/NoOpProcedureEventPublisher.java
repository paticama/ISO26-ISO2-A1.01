package com.sescam.radiodiagnostics.diagnostics.internal;

import com.sescam.radiodiagnostics.common.diagnostics.ProcedureEventPublisher;
import com.sescam.radiodiagnostics.common.diagnostics.ProcedureExecutionEvent;

/**
 * Stand-in for the subscribers in C4 (order status) and C5 (rescheduling, 2b): drops the event.
 */
public class NoOpProcedureEventPublisher implements ProcedureEventPublisher {

    @Override
    public void publishExecutionRecorded(ProcedureExecutionEvent event) {
        // intentionally empty
    }
}
