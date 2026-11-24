package com.sescam.radiodiagnostics.common.integration;

/**
 * Connection settings typed by the administrator. The {@code secret} is write-only: it travels from the
 * client to the server and is never returned, displayed, logged or put in an error message (A4).
 * A blank {@code secret} on an update means "keep the stored credential".
 */
public record IntegrationSettingsDTO(
        String endpointUrl,
        String username,
        String secret
) {
    /** Masks the secret so that logging a request can never reveal it. */
    @Override
    public String toString() {
        return "IntegrationSettingsDTO[endpointUrl=" + endpointUrl + ", username=" + username
                + ", secret=" + (secret == null || secret.isBlank() ? "<none>" : "****") + "]";
    }
}
