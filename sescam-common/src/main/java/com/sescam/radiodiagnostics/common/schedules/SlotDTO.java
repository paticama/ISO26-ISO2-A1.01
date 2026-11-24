package com.sescam.radiodiagnostics.common.schedules;

import java.time.LocalDateTime;

/**
 * One bookable time slot of a schedule. {@code version} is the value UC-AAD-05 passes to
 * {@link ScheduleService#reserveSlotAtomically} to detect stale slots.
 *
 * @param bookable true when the schedule is open, the slot is in the future and it has free capacity
 */
public record SlotDTO(
        String slotId,
        String scheduleId,
        String resourceId,
        String locationId,
        String modality,
        LocalDateTime start,
        LocalDateTime end,
        int capacity,
        int bookedCount,
        long version,
        boolean bookable
) {
    public int freeCapacity() {
        return Math.max(0, capacity - bookedCount);
    }
}
