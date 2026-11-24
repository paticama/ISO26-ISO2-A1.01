package com.sescam.radiodiagnostics.common.schedules;

/**
 * Operations of UC-AAD-02 that change a schedule. Used in audit entries and in {@link ScheduleChangedEvent}.
 */
public enum ScheduleOperation {
    OPEN,
    UPDATE,
    CLOSE,
    REOPEN
}
