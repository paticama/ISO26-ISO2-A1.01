package com.sescam.radiodiagnostics.common.schedules;

/**
 * Explicit error codes of UC-AAD-02 ("no schedule change is silently committed").
 * The first five come from the specification; the last three cover lookups, optimistic locking and the
 * booking hand-off used by UC-AAD-05.
 */
public enum ScheduleErrorCode {
    SCHEDULE_OVERLAP,
    CAPACITY_EXCEEDED,
    INVALID_SCHEDULE,
    APPOINTMENTS_AFFECTED,
    UNAUTHORIZED_OPERATION,
    SCHEDULE_NOT_FOUND,
    CONCURRENT_MODIFICATION,
    SLOT_UNAVAILABLE
}
