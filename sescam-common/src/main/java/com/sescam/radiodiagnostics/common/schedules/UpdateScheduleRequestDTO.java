package com.sescam.radiodiagnostics.common.schedules;

import java.time.LocalDateTime;

/**
 * Steps 3-4 (update): changes the time window, slot duration or capacity of an open schedule.
 * Resource, location and modality cannot change: open a new schedule instead.
 *
 * @param expectedVersion             version the client read (optimistic locking)
 * @param confirmAffectedAppointments false: if appointments are affected, nothing is saved and they are
 *                                    listed (A2). true: the change is saved and they are handed to admissions
 */
public record UpdateScheduleRequestDTO(
        String scheduleId,
        LocalDateTime start,
        LocalDateTime end,
        int slotDurationMinutes,
        int capacityPerSlot,
        long expectedVersion,
        boolean confirmAffectedAppointments,
        String reason,
        String actorId,
        String actorRole
) {
}
