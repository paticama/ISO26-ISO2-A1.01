package com.sescam.radiodiagnostics.common.orders;

/**
 * Public service contract for C4 (sescam-orders) realizing UC-PHY-01: Order and Prescribe Diagnostic Procedure.
 *
 * <p>The catalogue of examination types shown to the physician (step 2 of the use case) is read from C2 through
 * {@code ExamTypeService}, not from this interface. Later use cases of C4 (PHY-02, PHY-03, PAT-04) add methods here.
 */
public interface OrderService {

    /**
     * Creates a diagnostic order in {@link OrderStatus#PENDING_SCHEDULING} status.
     *
     * <p>Sequence: role check, mandatory-data validation (A4a), examination lookup, safety verification against the
     * patient's EHR profile (include: Verify Patient Safety Constraints and Contrast Allergies), consent and
     * preparation attachment (A6a), persistence, audit entry and publication of {@link OrderCreatedEvent}.
     *
     * <p>Safety flow (A6b): if the patient has constraints incompatible with the examination and
     * {@link CreateOrderDTO#overrideJustification()} is blank, nothing is persisted and a
     * {@link SafetyAlertException} carrying the constraints is thrown. The client shows the alert and resubmits the same
     * request with a justification (override) or cancels. With a justification the order is persisted with
     * {@code overrideApproved = true} and the override is audited.
     *
     * @return the stored order, including its tracking number and, if the examination needs it, the consent and
     *         preparation attachment
     * @throws UnauthorizedOrderAccessException if the actor may not order procedures
     * @throws InvalidOrderDataException        if indication, justification, priority, patient or examination are missing
     * @throws com.sescam.radiodiagnostics.common.catalogue.ExamTypeNotFoundException if the examination type does not
     *                                          exist or is no longer active
     * @throws SafetyAlertException             if a safety constraint is detected and no override was supplied
     */
    OrderResponseDTO createOrder(CreateOrderDTO request);
}
