package com.sescam.radiodiagnostics.common.orders;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;

import java.time.Instant;

/**
 * Stored order returned to the client (step 8 of UC-PHY-01).
 *
 * @param orderId        internal id
 * @param orderNumber    tracking identifier shown to the physician
 * @param consent        consent and preparation attachment; {@code null} when the examination needs neither (A6a)
 * @param overrideApproved {@code true} if the order was accepted despite a safety alert (A6b)
 * @param overrideJustification justification given by the physician; {@code null} when there was no override
 * @param orderedBy      physician that created the order
 * @param version        optimistic-locking version, 1 when created
 */
public record OrderResponseDTO(
        String orderId,
        String orderNumber,
        String patientId,
        String examTypeCode,
        String examTypeName,
        ModalityType modality,
        String clinicalIndication,
        String clinicalJustification,
        PriorityLevel priority,
        OrderStatus status,
        boolean requiresContrast,
        ConsentAndPreparationDTO consent,
        boolean overrideApproved,
        String overrideJustification,
        String orderedBy,
        Instant orderedAt,
        long version
) {
}
