package com.sescam.radiodiagnostics.common.schedules;

import java.time.LocalDate;

/**
 * Step 1: Admissions Staff selects a date, resource or location. Null fields do not filter.
 */
public record ScheduleSearchCriteriaDTO(
        LocalDate date,
        String resourceId,
        String locationId,
        ScheduleStatus status
) {
    public static ScheduleSearchCriteriaDTO all() {
        return new ScheduleSearchCriteriaDTO(null, null, null, null);
    }
}
