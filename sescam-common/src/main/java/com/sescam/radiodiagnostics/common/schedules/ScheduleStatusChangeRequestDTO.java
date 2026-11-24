package com.sescam.radiodiagnostics.common.schedules;

/**
 * Step 4 (close or reopen).
 *
 * @param expectedVersion             version the client read (optimistic locking)
 * @param confirmAffectedAppointments only for close: must be true when the schedule holds appointments (A2)
 * @param reason                      mandatory for close, stored in the audit entry
 */
public record ScheduleStatusChangeRequestDTO(
        String scheduleId,
        long expectedVersion,
        boolean confirmAffectedAppointments,
        String reason,
        String actorId,
        String actorRole
) {
}
