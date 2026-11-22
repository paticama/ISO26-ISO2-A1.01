package com.sescam.radiodiagnostics.integration.internal;

import com.sescam.radiodiagnostics.common.integration.IntegrationSettingsDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Validation rules for integration settings (A2). The messages never contain the secret.
 */
public class SettingsValidator {

    static final int MAX_URL = 2048;
    static final int MAX_USERNAME = 100;
    static final int MIN_SECRET = 8;
    static final int MAX_SECRET = 256;

    /**
     * @param credentialAlreadyStored true when the integration already has a credential, so a blank secret
     *                                means "keep it"
     * @return the list of problems; empty when the settings are valid
     */
    public List<String> validate(IntegrationSettingsDTO settings, boolean credentialAlreadyStored) {
        List<String> errors = new ArrayList<>();
        if (settings == null) {
            errors.add("Settings are required.");
            return errors;
        }
        validateEndpoint(settings.endpointUrl(), errors);
        validateUsername(settings.username(), errors);
        validateSecret(settings.secret(), credentialAlreadyStored, errors);
        return errors;
    }

    private void validateEndpoint(String endpoint, List<String> errors) {
        if (endpoint == null || endpoint.isBlank()) {
            errors.add("Endpoint URL is required.");
            return;
        }
        String url = endpoint.trim();
        if (url.length() > MAX_URL) {
            errors.add("Endpoint URL must have at most " + MAX_URL + " characters.");
            return;
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException ex) {
            errors.add("Endpoint URL is not a valid URL.");
            return;
        }
        if (uri.getScheme() == null || !"https".equals(uri.getScheme().toLowerCase(Locale.ROOT))) {
            errors.add("Endpoint URL must use https.");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            errors.add("Endpoint URL must include a host name.");
        }
        if (uri.getUserInfo() != null) {
            errors.add("Do not put credentials in the endpoint URL; use the credential field.");
        }
        if (uri.getPort() > 65535 || uri.getPort() == 0) {
            errors.add("Endpoint URL has an invalid port.");
        }
    }

    private void validateUsername(String username, List<String> errors) {
        if (username == null || username.isBlank()) {
            return;
        }
        if (username.length() > MAX_USERNAME) {
            errors.add("User name must have at most " + MAX_USERNAME + " characters.");
        }
        if (username.chars().anyMatch(Character::isISOControl)) {
            errors.add("User name must not contain control characters.");
        }
    }

    private void validateSecret(String secret, boolean credentialAlreadyStored, List<String> errors) {
        if (secret == null || secret.isBlank()) {
            if (!credentialAlreadyStored) {
                errors.add("A credential is required the first time an integration is configured.");
            }
            return;
        }
        if (secret.length() < MIN_SECRET || secret.length() > MAX_SECRET) {
            errors.add("Credential must have between " + MIN_SECRET + " and " + MAX_SECRET + " characters.");
        }
    }
}
