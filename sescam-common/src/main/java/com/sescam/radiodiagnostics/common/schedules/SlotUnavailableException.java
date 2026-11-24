package com.sescam.radiodiagnostics.common.schedules;

/**
 * The slot cannot be reserved: its schedule is closed, the slot is full or it is in the past.
 */
public class SlotUnavailableException extends ScheduleException {

    public SlotUnavailableException(String message) {
        super(ScheduleErrorCode.SLOT_UNAVAILABLE, message);
    }
}
