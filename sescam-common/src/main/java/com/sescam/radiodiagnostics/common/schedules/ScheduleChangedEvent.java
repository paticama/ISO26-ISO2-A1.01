package com.sescam.radiodiagnostics.common.schedules;

import java.time.Instant;
import java.util.List;

/**
 * Event published by C3 after every committed schedule change (ADR-014: hand-offs between modules may use
 * events). C5 (sescam-admissions) listens to it to reschedule or cancel the affected appointments and to
 * refresh the daily worklist (UC-AAD-04).
 */
public record ScheduleChangedEvent(
        String scheduleId,
        ScheduleOperation operation,
        ScheduleStatus newStatus,
        List<AffectedAppointmentDTO> affectedAppointments,
        String actorId,
        Instant occurredAt
) {
    public ScheduleChangedEvent {
        affectedAppointments = affectedAppointments == null ? List.of() : List.copyOf(affectedAppointments);
    }

    public boolean requiresRescheduling() {
        return !affectedAppointments.isEmpty();
    }
}
