package com.sescam.radiodiagnostics.common.orders;

/**
 * A patient safety constraint that is active in the EHR, or the part of it that is incompatible with an examination
 * when carried by {@link SafetyAlertException}.
 *
 * @param constraintId              EHR identifier of the constraint
 * @param type                      kind of constraint
 * @param absoluteContraindication  {@code true} if the constraint is an absolute contraindication
 * @param description               clinical description shown in the alert
 */
public record SafetyConstraintDTO(
        String constraintId,
        ConstraintType type,
        boolean absoluteContraindication,
        String description
) {
}
