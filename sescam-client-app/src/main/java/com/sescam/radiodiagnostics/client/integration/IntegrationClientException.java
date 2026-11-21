package com.sescam.radiodiagnostics.client.integration;

import java.util.List;

/**
 * Error returned by the server integrations API, translated for the view (A1, A2, A3).
 * It never carries settings or credentials, only the messages the server chose to show.
 */
public class IntegrationClientException extends RuntimeException {

    public enum Kind { FORBIDDEN, NOT_FOUND, INVALID_SETTINGS, CONNECTION_FAILED, CONFLICT, SAVE_FAILED, SERVER_UNREACHABLE }

    private final Kind kind;
    private final List<String> errors;
    private final String connectionStatus;

    public IntegrationClientException(Kind kind, String message) {
        this(kind, message, List.of(), null, null);
    }

    public IntegrationClientException(Kind kind, String message, List<String> errors, String connectionStatus,
                                      Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.errors = errors == null ? List.of() : List.copyOf(errors);
        this.connectionStatus = connectionStatus;
    }

    public Kind getKind() {
        return kind;
    }

    /** Validation messages when the kind is INVALID_SETTINGS. */
    public List<String> getErrors() {
        return errors;
    }

    /** CONNECTION_FAILED (refused) or UNAVAILABLE (unreachable) when the kind is CONNECTION_FAILED. */
    public String getConnectionStatus() {
        return connectionStatus;
    }

    /** True when the administrator can simply try again (A3). */
    public boolean canRetry() {
        return kind == Kind.SERVER_UNREACHABLE || kind == Kind.SAVE_FAILED
                || (kind == Kind.CONNECTION_FAILED && "UNAVAILABLE".equals(connectionStatus));
    }

    /** True when the administrator has to change the settings (A2). */
    public boolean mustFixSettings() {
        return kind == Kind.INVALID_SETTINGS
                || (kind == Kind.CONNECTION_FAILED && !"UNAVAILABLE".equals(connectionStatus));
    }
}
