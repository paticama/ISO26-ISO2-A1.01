package com.sescam.radiodiagnostics.common.schedules;

import java.util.List;

/**
 * A1: the time window overlaps another open schedule of the same resource. The change is rejected and the
 * conflicting schedules are returned so the client can display them.
 */
public class ScheduleOverlapException extends ScheduleException {

    private final List<ScheduleSummaryDTO> conflictingSchedules;

    public ScheduleOverlapException(String message, List<ScheduleSummaryDTO> conflictingSchedules) {
        super(ScheduleErrorCode.SCHEDULE_OVERLAP, message);
        this.conflictingSchedules = conflictingSchedules == null ? List.of() : List.copyOf(conflictingSchedules);
    }

    public List<ScheduleSummaryDTO> getConflictingSchedules() {
        return conflictingSchedules;
    }
}
