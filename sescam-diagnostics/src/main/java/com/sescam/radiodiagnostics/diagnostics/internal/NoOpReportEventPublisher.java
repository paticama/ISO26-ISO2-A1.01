package com.sescam.radiodiagnostics.diagnostics.internal;

import com.sescam.radiodiagnostics.common.diagnostics.ReportAvailabilityEvent;
import com.sescam.radiodiagnostics.common.diagnostics.ReportEventPublisher;

/**
 * TEMPORARY stand-in for downstream report availability subscribers (C8 patient notifications).
 */
public class NoOpReportEventPublisher implements ReportEventPublisher {

    @Override
    public void publishReportAvailable(ReportAvailabilityEvent event) {
        // C8 subscribes here in Iteration 3
    }
}
