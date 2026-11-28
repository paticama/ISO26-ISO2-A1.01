package com.sescam.radiodiagnostics.common.schedules;

/**
 * The schedule or slot does not exist.
 */
public class ScheduleNotFoundException extends ScheduleException {

    public ScheduleNotFoundException(String message) {
        super(ScheduleErrorCode.SCHEDULE_NOT_FOUND, message);
    }
}
