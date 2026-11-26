package com.sescam.radiodiagnostics.client.diagnostics;

import java.util.List;

/**
 * Error returned by the UC-PHY-04 server API, translated for the client views.
 */
public class DiagnosticReportClientException extends RuntimeException {

    public enum Kind {
        FORBIDDEN,
        INVALID_DATA,
        NOT_FOUND,
        STATE_CONFLICT,
        PACS_ERROR,
        FAILED,
        SERVER_UNREACHABLE
    }

    private final Kind kind;
    private final List<String> invalidFields;

    public DiagnosticReportClientException(Kind kind, String message) {
        this(kind, message, List.of(), null);
    }

    public DiagnosticReportClientException(Kind kind, String message, List<String> invalidFields, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.invalidFields = invalidFields == null ? List.of() : List.copyOf(invalidFields);
    }

    public Kind getKind() {
        return kind;
    }

    public List<String> getInvalidFields() {
        return invalidFields;
    }

    public boolean canRetry() {
        return kind == Kind.PACS_ERROR || kind == Kind.FAILED || kind == Kind.SERVER_UNREACHABLE;
    }
}
