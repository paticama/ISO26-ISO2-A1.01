package com.sescam.radiodiagnostics.common.schedules;

import java.time.LocalDateTime;

/**
 * Steps 3-4 (open): creates a new open schedule.
 *
 * @param resourceId          room or equipment id
 * @param locationId          hospital / site id
 * @param modality            examination modality served (e.g. MRI, CT); free text until C2 publishes the
 *                            modality catalogue
 * @param start               start of the time window (inclusive)
 * @param end                 end of the time window (exclusive), same day as {@code start}
 * @param slotDurationMinutes duration of every slot; must divide the window exactly
 * @param capacityPerSlot     places per slot (at least 1, at most the limit configured in C2)
 */
public record OpenScheduleRequestDTO(
        String resourceId,
        String locationId,
        String modality,
        LocalDateTime start,
        LocalDateTime end,
        int slotDurationMinutes,
        int capacityPerSlot,
        String actorId,
        String actorRole
) {
}
