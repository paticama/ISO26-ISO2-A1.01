package com.sescam.radiodiagnostics.schedules.internal;

import com.sescam.radiodiagnostics.common.schedules.ScheduleChangedEvent;
import com.sescam.radiodiagnostics.common.schedules.ScheduleEventPublisher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stand-in for C5 (sescam-admissions) until UC-AAD-05 implements the rescheduling hand-off (ADR-014). It only
 * keeps the published events, so they can be inspected.
 */
public class NoOpScheduleEventPublisher implements ScheduleEventPublisher {

    private final List<ScheduleChangedEvent> published = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void publish(ScheduleChangedEvent event) {
        published.add(event);
    }

    public List<ScheduleChangedEvent> getPublished() {
        return List.copyOf(published);
    }
}
