package com.sescam.radiodiagnostics.common.schedules;

import java.util.List;

/**
 * Result of a schedule change (step 7): the resulting availability and the rescheduling actions it needs.
 *
 * @param operation            what was done
 * @param schedule             the schedule after the change, with its slots
 * @param affectedAppointments appointments affected by the change (empty if none)
 * @param reschedulingRequired true when admissions has to reschedule or cancel some appointment
 */
public record ScheduleChangeResultDTO(
        ScheduleOperation operation,
        ScheduleDTO schedule,
        List<AffectedAppointmentDTO> affectedAppointments,
        boolean reschedulingRequired
) {
    public ScheduleChangeResultDTO {
        affectedAppointments = affectedAppointments == null ? List.of() : List.copyOf(affectedAppointments);
    }
}
