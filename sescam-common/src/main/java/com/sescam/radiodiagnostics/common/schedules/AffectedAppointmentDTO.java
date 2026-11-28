package com.sescam.radiodiagnostics.common.schedules;

import java.time.LocalDateTime;

/**
 * An appointment (booking of C5) that holds a place in a slot affected by a schedule change.
 *
 * @param bookingReference id of the booking in C5 (sescam-admissions), as given to
 *                         {@link ScheduleService#reserveSlotAtomically}
 * @param reason           why it is affected, e.g. "Slot removed by the new time window"
 */
public record AffectedAppointmentDTO(
        String bookingReference,
        String scheduleId,
        String slotId,
        LocalDateTime slotStart,
        LocalDateTime slotEnd,
        String reason
) {
}
