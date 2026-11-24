package com.sescam.radiodiagnostics.common.orders;

/**
 * Prescription payload sent by the client (step 5 of UC-PHY-01).
 *
 * @param patientId             internal patient id (selected from the EHR); mandatory
 * @param examTypeCode          code of an active examination type of the catalogue; mandatory
 * @param clinicalIndication    mandatory
 * @param clinicalJustification mandatory
 * @param priority              mandatory
 * @param overrideJustification clinical justification to proceed despite a safety alert (A6b); {@code null} or blank on
 *                              the first submission
 * @param actorId               authenticated physician
 * @param actorRole             role of the authenticated physician
 */
public record CreateOrderDTO(
        String patientId,
        String examTypeCode,
        String clinicalIndication,
        String clinicalJustification,
        PriorityLevel priority,
        String overrideJustification,
        String actorId,
        String actorRole
) {
}
