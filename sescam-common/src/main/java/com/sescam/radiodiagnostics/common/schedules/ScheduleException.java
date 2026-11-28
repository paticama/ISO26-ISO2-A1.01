package com.sescam.radiodiagnostics.common.schedules;

/**
 * Base class of the UC-AAD-02 exceptions. Each subclass carries one {@link ScheduleErrorCode}, which the
 * REST layer returns to the client.
 */
public abstract class ScheduleException extends RuntimeException {

    private final ScheduleErrorCode code;

    protected ScheduleException(ScheduleErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ScheduleErrorCode getCode() {
        return code;
    }
}
