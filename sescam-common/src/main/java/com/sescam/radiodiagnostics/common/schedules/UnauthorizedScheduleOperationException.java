package com.sescam.radiodiagnostics.common.schedules;

/**
 * A6: the actor's role may not perform this schedule operation.
 */
public class UnauthorizedScheduleOperationException extends ScheduleException {

    public UnauthorizedScheduleOperationException(String message) {
        super(ScheduleErrorCode.UNAUTHORIZED_OPERATION, message);
    }
}
