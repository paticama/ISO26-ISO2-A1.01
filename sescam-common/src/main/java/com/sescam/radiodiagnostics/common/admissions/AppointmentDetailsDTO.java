package com.sescam.radiodiagnostics.common.admissions;

import com.sescam.radiodiagnostics.common.orders.ConsentAndPreparationDTO;
import com.sescam.radiodiagnostics.common.orders.PriorityLevel;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Details of one appointment for the patient (UC-PAT-01 steps 5-6).
 *
 * @param priority       clinical priority of the order; STAT_URGENT or PREFERENTIAL makes the client warn
 *                       before cancelling (A4). Null if the order is not available.
 * @param preparation    preparation instructions of the examination type (C2); null if the catalogue has none
 * @param allowedActions actions the patient may take now; empty for CANCELLED or EXECUTED appointments (A7)
 * @param pendingRequest the rescheduling request waiting for Admissions, or null (A2, A6)
 * @param serviceContact how to contact the service when an action is not allowed (A4, A5)
 * @param version        booking version the client must send back to confirm or cancel
 */
public record AppointmentDetailsDTO(
        AppointmentSummaryDTO summary,
        String examTypeCode,
        PriorityLevel priority,
        ConsentAndPreparationDTO preparation,
        Set<AppointmentAction> allowedActions,
        RescheduleRequestStatusDTO pendingRequest,
        String serviceContact,
        long version
) {
    public AppointmentDetailsDTO {
        // Immutable copy that keeps the enum order, so clients always receive the actions in the same order.
        allowedActions = allowedActions == null || allowedActions.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(allowedActions));
    }
}
