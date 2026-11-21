package com.sescam.radiodiagnostics.common.integration;

/**
 * A2/A3: the connection check with the proposed settings failed. The working configuration was not replaced.
 * {@link #getStatus()} tells apart "refused" ({@code CONNECTION_FAILED}) from "unreachable" ({@code UNAVAILABLE}).
 */
public class IntegrationConnectionFailedException extends RuntimeException {
    private final String integrationId;
    private final IntegrationStatus status;

    public IntegrationConnectionFailedException(String integrationId, IntegrationStatus status, String message) {
        super(message);
        this.integrationId = integrationId;
        this.status = status;
    }

    public String getIntegrationId() {
        return integrationId;
    }

    public IntegrationStatus getStatus() {
        return status;
    }
}
