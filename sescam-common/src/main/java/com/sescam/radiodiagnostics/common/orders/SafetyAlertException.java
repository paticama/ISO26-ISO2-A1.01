package com.sescam.radiodiagnostics.common.orders;

import java.util.List;

/**
 * A6b: the patient has safety constraints incompatible with the examination and the request has no override
 * justification. Nothing was stored. HTTP 422 with the constraints in the body so the client can show the alert.
 */
public class SafetyAlertException extends RuntimeException {

    private final List<SafetyConstraintDTO> constraints;

    public SafetyAlertException(String message, List<SafetyConstraintDTO> constraints) {
        super(message);
        this.constraints = List.copyOf(constraints);
    }

    /** The incompatible constraints, never empty. */
    public List<SafetyConstraintDTO> getConstraints() {
        return constraints;
    }
}
