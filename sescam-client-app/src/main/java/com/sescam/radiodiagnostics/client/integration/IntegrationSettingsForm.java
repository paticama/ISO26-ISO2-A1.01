package com.sescam.radiodiagnostics.client.integration;

import com.sescam.radiodiagnostics.common.integration.IntegrationSettingsDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Form of the integration settings with the field validations done in the client before calling the server.
 * The credential is never put back into the page: {@link #toString()} masks it and the template has no value
 * for the password field (A4).
 */
public class IntegrationSettingsForm {

    private String endpointUrl;
    private String username;
    private String secret;
    private long expectedVersion;

    public Map<String, String> validate(boolean credentialAlreadyStored) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateEndpoint(errors);
        if (username != null && !username.isBlank()) {
            if (username.length() > 100) {
                errors.put("username", "User name must have at most 100 characters.");
            } else if (username.chars().anyMatch(Character::isISOControl)) {
                errors.put("username", "User name must not contain control characters.");
            }
        }
        if (secret == null || secret.isBlank()) {
            if (!credentialAlreadyStored) {
                errors.put("secret", "A credential is required the first time an integration is configured.");
            }
        } else if (secret.length() < 8 || secret.length() > 256) {
            errors.put("secret", "Credential must have between 8 and 256 characters.");
        }
        return errors;
    }

    private void validateEndpoint(Map<String, String> errors) {
        if (endpointUrl == null || endpointUrl.isBlank()) {
            errors.put("endpointUrl", "Endpoint URL is required.");
            return;
        }
        String url = endpointUrl.trim();
        if (url.length() > 2048) {
            errors.put("endpointUrl", "Endpoint URL must have at most 2048 characters.");
            return;
        }
        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null || !"https".equals(uri.getScheme().toLowerCase(Locale.ROOT))) {
                errors.put("endpointUrl", "Endpoint URL must use https.");
            } else if (uri.getHost() == null || uri.getHost().isBlank()) {
                errors.put("endpointUrl", "Endpoint URL must include a host name.");
            } else if (uri.getUserInfo() != null) {
                errors.put("endpointUrl", "Do not put credentials in the endpoint URL; use the credential field.");
            } else if (uri.getPort() > 65535 || uri.getPort() == 0) {
                errors.put("endpointUrl", "Endpoint URL has an invalid port.");
            }
        } catch (URISyntaxException ex) {
            errors.put("endpointUrl", "Endpoint URL is not a valid URL.");
        }
    }

    public IntegrationSettingsDTO toSettings() {
        return new IntegrationSettingsDTO(endpointUrl == null ? null : endpointUrl.trim(), username, secret);
    }

    /** Removes the credential from the form once it has been sent. */
    public void clearSecret() {
        this.secret = null;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }

    @Override
    public String toString() {
        return "IntegrationSettingsForm[endpointUrl=" + endpointUrl + ", username=" + username
                + ", secret=" + (secret == null || secret.isBlank() ? "<none>" : "****")
                + ", expectedVersion=" + expectedVersion + "]";
    }
}
