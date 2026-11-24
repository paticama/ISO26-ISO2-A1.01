package com.sescam.radiodiagnostics.common.schedules;

/**
 * Outbound port of C3 for the rescheduling hand-off ("Booking interface" of UC-AAD-02). Implemented by C5
 * (sescam-admissions) once it exists; until then C3 uses a no-op implementation (ADR-014).
 */
public interface ScheduleEventPublisher {

    void publish(ScheduleChangedEvent event);
}
