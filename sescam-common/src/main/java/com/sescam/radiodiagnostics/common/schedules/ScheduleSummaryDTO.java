package com.sescam.radiodiagnostics.common.schedules;

import java.time.LocalDateTime;

/**
 * Short view of a schedule, used in search results and in the conflicts of {@link ScheduleOverlapException}.
 */
public record ScheduleSummaryDTO(
        String scheduleId,
        String resourceId,
        String locationId,
        String modality,
        LocalDateTime start,
        LocalDateTime end,
        ScheduleStatus status,
        int totalCapacity,
        int bookedCount
) {
}
