package com.sescam.radiodiagnostics.common.integration;

import java.util.List;

/**
 * A2: the proposed settings are not valid. The working configuration was not replaced.
 */
public class InvalidIntegrationSettingsException extends RuntimeException {
    private final String integrationId;
    private final List<String> errors;

    public InvalidIntegrationSettingsException(String integrationId, List<String> errors) {
        super("Invalid settings for integration '" + integrationId + "': " + String.join(" ", errors));
        this.integrationId = integrationId;
        this.errors = List.copyOf(errors);
    }

    public String getIntegrationId() {
        return integrationId;
    }

    public List<String> getErrors() {
        return errors;
    }
}
