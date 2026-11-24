package com.sescam.radiodiagnostics.common.schedules;

/**
 * A5: invalid dates, resource, location, slot duration or capacity. The change is not saved.
 */
public class InvalidScheduleException extends ScheduleException {

    public InvalidScheduleException(String message) {
        super(ScheduleErrorCode.INVALID_SCHEDULE, message);
    }
}
