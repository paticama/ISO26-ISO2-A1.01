package com.sescam.radiodiagnostics.common.diagnostics;

/**
 * Outbound event port for notifying downstream modules (e.g., C8 patient notifications) when a report is finalized.
 */
public interface ReportEventPublisher {

    void publishReportAvailable(ReportAvailabilityEvent event);
}
