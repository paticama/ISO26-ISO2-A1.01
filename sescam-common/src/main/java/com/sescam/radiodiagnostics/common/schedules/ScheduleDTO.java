package com.sescam.radiodiagnostics.common.schedules;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full view of an examination schedule: time window on one resource (room or equipment) at one location,
 * divided into slots of {@code slotDurationMinutes}, each one with {@code capacityPerSlot} places.
 *
 * @param appointmentsPendingRescheduling appointments taken out of the schedule by a confirmed change (A2)
 *                                        and handed to admissions for rescheduling or cancellation
 */
public record ScheduleDTO(
        String scheduleId,
        String resourceId,
        String locationId,
        String modality,
        LocalDateTime start,
        LocalDateTime end,
        int slotDurationMinutes,
        int capacityPerSlot,
        ScheduleStatus status,
        long version,
        List<SlotDTO> slots,
        List<AffectedAppointmentDTO> appointmentsPendingRescheduling,
        String lastChangeReason,
        Instant updatedAt
) {
    public ScheduleDTO {
        slots = slots == null ? List.of() : List.copyOf(slots);
        appointmentsPendingRescheduling = appointmentsPendingRescheduling == null
                ? List.of() : List.copyOf(appointmentsPendingRescheduling);
    }
}
