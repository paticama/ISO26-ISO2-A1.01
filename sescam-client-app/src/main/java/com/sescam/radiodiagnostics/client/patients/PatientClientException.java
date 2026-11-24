package com.sescam.radiodiagnostics.client.patients;

import java.util.List;

public class PatientClientException extends RuntimeException {

    public enum Kind {
        FORBIDDEN,
        NOT_FOUND,
        CONFLICT,
        INVALID_DATA,
        EHR_SYNC_FAILED,
        SERVER_UNREACHABLE,
        GENERIC_ERROR
    }

    private final Kind kind;
    private final List<String> errors;

    public PatientClientException(Kind kind, String message) {
        this(kind, message, List.of(), null);
    }

    public PatientClientException(Kind kind, String message, List<String> errors, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public Kind getKind() {
        return kind;
    }

    public List<String> getErrors() {
        return errors;
    }
}
