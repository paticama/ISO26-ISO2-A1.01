package com.sescam.radiodiagnostics.common.integration;

/**
 * Connection status of an integration. A failure is never reported as a success (UC-ADM-04, A2 and A3).
 */
public enum IntegrationStatus {
    /** No settings have been saved yet. */
    NOT_CONFIGURED,
    /** Settings are saved, but this integration has no connection check yet (or it is not supported). */
    NOT_CHECKED,
    /** The last connection check succeeded. */
    CONNECTED,
    /** The external system answered, but refused the settings or the credentials. */
    CONNECTION_FAILED,
    /** The external system could not be reached. The administrator can retry. */
    UNAVAILABLE;

    /** True when the last check shows that the connection does not work. */
    public boolean isFailure() {
        return this == CONNECTION_FAILED || this == UNAVAILABLE;
    }
}
