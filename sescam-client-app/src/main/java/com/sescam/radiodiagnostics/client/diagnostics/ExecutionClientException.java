package com.sescam.radiodiagnostics.client.diagnostics;

import java.util.List;

/**
 * Error returned by the UC-PHY-05 server API, translated for the view.
 */
public class ExecutionClientException extends RuntimeException {

    public enum Kind { FORBIDDEN, INVALID_DATA, PROCEDURE_NOT_FOUND, ALREADY_RECORDED, FAILED, SERVER_UNREACHABLE }

    private final Kind kind;
    private final List<String> invalidFields;

    public ExecutionClientException(Kind kind, String message) {
        this(kind, message, List.of(), null);
    }

    public ExecutionClientException(Kind kind, String message, List<String> invalidFields, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.invalidFields = List.copyOf(invalidFields);
    }

    public Kind getKind() {
        return kind;
    }

    /** Fields rejected by the server (INVALID_DATA); empty otherwise. */
    public List<String> getInvalidFields() {
        return invalidFields;
    }

    /** True when the user can simply send the log again. */
    public boolean canRetry() {
        return kind == Kind.FAILED || kind == Kind.SERVER_UNREACHABLE;
    }
}
