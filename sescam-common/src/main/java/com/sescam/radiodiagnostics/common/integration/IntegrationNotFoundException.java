package com.sescam.radiodiagnostics.common.integration;

public class IntegrationNotFoundException extends RuntimeException {
    private final String integrationId;

    public IntegrationNotFoundException(String integrationId) {
        super("Integration '" + integrationId + "' does not exist.");
        this.integrationId = integrationId;
    }

    public String getIntegrationId() {
        return integrationId;
    }
}
