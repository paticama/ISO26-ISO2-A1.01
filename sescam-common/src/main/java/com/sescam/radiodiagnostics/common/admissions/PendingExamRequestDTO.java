package com.sescam.radiodiagnostics.common.admissions;

import com.sescam.radiodiagnostics.common.orders.PriorityLevel;

import java.time.Instant;

/** Pending diagnostic examination request awaiting booking by Admissions Staff (step 1 of UC-AAD-05). */
public record PendingExamRequestDTO(
        String orderId,
        String orderNumber,
        String patientId,
        String examTypeCode,
        String examTypeName,
        String modality,
        PriorityLevel priority,
        Instant orderedAt
) {
}
