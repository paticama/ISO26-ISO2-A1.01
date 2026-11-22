package com.sescam.radiodiagnostics.integration.internal;

import com.sescam.radiodiagnostics.common.integration.IntegrationSettingsDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SettingsValidatorTest {

    private final SettingsValidator validator = new SettingsValidator();

    private static IntegrationSettingsDTO s(String url, String user, String secret) {
        return new IntegrationSettingsDTO(url, user, secret);
    }

    @Test
    @DisplayName("Valid https settings with credential pass")
    void validSettings() {
        assertThat(validator.validate(s("https://ehr.hospital.example:8443/fhir", "svc", "Abcdefg-123"), false)).isEmpty();
    }

    @Test
    @DisplayName("Null settings are rejected")
    void nullSettings() {
        assertThat(validator.validate(null, false)).containsExactly("Settings are required.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Endpoint is required")
    void endpointRequired(String url) {
        assertThat(validator.validate(s(url, null, "Abcdefg-123"), false)).containsExactly("Endpoint URL is required.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://plain.example", "ftp://files.example", "sso.hospital.example", "https://"})
    @DisplayName("Endpoint must be https with a host")
    void endpointMustBeHttpsWithHost(String url) {
        assertThat(validator.validate(s(url, null, "Abcdefg-123"), false)).isNotEmpty();
    }

    @Test
    @DisplayName("Endpoint with embedded credentials, bad syntax, bad port or huge length is rejected")
    void endpointOtherProblems() {
        assertThat(validator.validate(s("https://user:pw@host.example", null, "Abcdefg-123"), false))
                .anyMatch(e -> e.contains("credential field"));
        assertThat(validator.validate(s("https://bad host", null, "Abcdefg-123"), false))
                .contains("Endpoint URL is not a valid URL.");
        assertThat(validator.validate(s("https://host.example:70000", null, "Abcdefg-123"), false))
                .contains("Endpoint URL has an invalid port.");
        assertThat(validator.validate(s("https://host.example/" + "a".repeat(2100), null, "Abcdefg-123"), false))
                .anyMatch(e -> e.contains("at most 2048"));
    }

    @Test
    @DisplayName("User name is optional but limited and without control characters")
    void usernameRules() {
        assertThat(validator.validate(s("https://h.example", "", "Abcdefg-123"), false)).isEmpty();
        assertThat(validator.validate(s("https://h.example", "u".repeat(101), "Abcdefg-123"), false))
                .anyMatch(e -> e.contains("at most 100"));
        assertThat(validator.validate(s("https://h.example", "bad\nname", "Abcdefg-123"), false))
                .anyMatch(e -> e.contains("control characters"));
    }

    @Test
    @DisplayName("Credential is required the first time, optional afterwards, and limited in length")
    void secretRules() {
        assertThat(validator.validate(s("https://h.example", null, null), false))
                .anyMatch(e -> e.contains("required the first time"));
        assertThat(validator.validate(s("https://h.example", null, "  "), true)).isEmpty();
        assertThat(validator.validate(s("https://h.example", null, "short"), true))
                .containsExactly("Credential must have between 8 and 256 characters.");
        assertThat(validator.validate(s("https://h.example", null, "x".repeat(257)), true))
                .containsExactly("Credential must have between 8 and 256 characters.");
    }

    @Test
    @DisplayName("Error messages never contain the credential")
    void messagesDoNotContainSecret() {
        List<String> errors = validator.validate(s("http://x", null, "tiny"), false);
        assertThat(String.join(" ", errors)).doesNotContain("tiny");
    }
}
