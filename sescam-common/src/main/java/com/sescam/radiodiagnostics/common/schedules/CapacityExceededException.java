package com.sescam.radiodiagnostics.common.schedules;

/**
 * The requested capacity is above the limit configured by the System Administrator, or a slot is already full.
 */
public class CapacityExceededException extends ScheduleException {

    public CapacityExceededException(String message) {
        super(ScheduleErrorCode.CAPACITY_EXCEEDED, message);
    }
}
