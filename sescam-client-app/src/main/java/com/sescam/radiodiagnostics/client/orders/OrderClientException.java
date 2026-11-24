package com.sescam.radiodiagnostics.client.orders;

import com.sescam.radiodiagnostics.common.orders.SafetyConstraintDTO;

import java.util.List;

/**
 * Error returned by the UC-PHY-01 server API, translated for the view (4a, 6b, 403).
 */
public class OrderClientException extends RuntimeException {

    public enum Kind { FORBIDDEN, INVALID_DATA, EXAM_NOT_FOUND, SAFETY_ALERT, FAILED, SERVER_UNREACHABLE }

    private final Kind kind;
    private final List<SafetyConstraintDTO> constraints;
    private final List<String> invalidFields;

    public OrderClientException(Kind kind, String message) {
        this(kind, message, List.of(), List.of(), null);
    }

    public OrderClientException(Kind kind, String message, Throwable cause) {
        this(kind, message, List.of(), List.of(), cause);
    }

    public OrderClientException(Kind kind, String message, List<SafetyConstraintDTO> constraints,
                                List<String> invalidFields, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.constraints = List.copyOf(constraints);
        this.invalidFields = List.copyOf(invalidFields);
    }

    public Kind getKind() {
        return kind;
    }

    /** 6b: the incompatible constraints to show in the alert; empty for any other kind. */
    public List<SafetyConstraintDTO> getConstraints() {
        return constraints;
    }

    /** 4a on the server: names of the fields the server rejected; empty for any other kind. */
    public List<String> getInvalidFields() {
        return invalidFields;
    }

    public boolean isSafetyAlert() {
        return kind == Kind.SAFETY_ALERT;
    }

    /** True when the physician can simply send the order again. */
    public boolean canRetry() {
        return kind == Kind.FAILED || kind == Kind.SERVER_UNREACHABLE;
    }
}
