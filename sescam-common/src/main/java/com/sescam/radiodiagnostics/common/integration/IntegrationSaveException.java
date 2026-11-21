package com.sescam.radiodiagnostics.common.integration;

/**
 * The settings could not be stored. The previous configuration stays active.
 * The cause is not copied into the message because it could contain settings.
 */
public class IntegrationSaveException extends RuntimeException {
    private final String integrationId;

    public IntegrationSaveException(String integrationId, Throwable cause) {
        super("Integration '" + integrationId + "' could not be saved. The previous configuration is still active.",
                cause);
        this.integrationId = integrationId;
    }

    public String getIntegrationId() {
        return integrationId;
    }
}
