package com.sescam.radiodiagnostics.common.admissions;

/** Request to book a pending examination into a compatible schedule slot (steps 4-6 of UC-AAD-05). */
public record CreateBookingRequestDTO(
        String orderId,
        String slotId,
        long expectedSlotVersion,
        String actorId,
        String actorRole
) {
}
