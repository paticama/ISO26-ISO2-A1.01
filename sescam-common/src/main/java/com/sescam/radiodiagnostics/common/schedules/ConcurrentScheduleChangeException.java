package com.sescam.radiodiagnostics.common.schedules;

/**
 * The schedule or slot was changed by someone else (optimistic locking).
 */
public class ConcurrentScheduleChangeException extends ScheduleException {

    public ConcurrentScheduleChangeException(String message) {
        super(ScheduleErrorCode.CONCURRENT_MODIFICATION, message);
    }
}
