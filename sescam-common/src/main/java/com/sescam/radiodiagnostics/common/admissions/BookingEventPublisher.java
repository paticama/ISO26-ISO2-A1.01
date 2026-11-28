package com.sescam.radiodiagnostics.common.admissions;

/** Outbound port for publishing booking lifecycle events (ADR-014). */
public interface BookingEventPublisher {

    void publish(BookingChangedEvent event);
}
