package com.sescam.radiodiagnostics.common.diagnostics;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;

import java.time.Instant;

/**
 * A booked procedure as C7 needs it to log its execution (read only; owned by C4 orders and C5 admissions).
 *
 * @param orderId                 order of UC-PHY-01; identifies the procedure
 * @param scheduledAt             start of the booked slot
 * @param roomId                  booked room
 * @param requiresContrast        the examination uses contrast medium
 * @param requiresInformedConsent the patient must have signed a consent
 */
public record ScheduledProcedureDTO(
        String orderId,
        String patientId,
        String examTypeCode,
        String examTypeName,
        ModalityType modality,
        Instant scheduledAt,
        String roomId,
        boolean requiresContrast,
        boolean requiresInformedConsent
) {
}
