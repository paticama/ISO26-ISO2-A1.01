package com.sescam.radiodiagnostics.common.orders;

import java.time.Instant;

/**
 * Event published by C4 when an order enters {@link OrderStatus#PENDING_SCHEDULING}. Carries what admissions needs to
 * allocate a slot, not the clinical text of the order.
 */
public record OrderCreatedEvent(
        String orderId,
        String orderNumber,
        String patientId,
        String examTypeCode,
        PriorityLevel priority,
        boolean requiresContrast,
        Instant orderedAt
) {
}
