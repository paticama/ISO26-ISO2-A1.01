package com.sescam.radiodiagnostics.client.integration;

import com.sescam.radiodiagnostics.common.integration.IntegrationSettingsDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationSettingsFormTest {

    private static IntegrationSettingsForm form(String url, String user, String secret) {
        IntegrationSettingsForm form = new IntegrationSettingsForm();
        form.setEndpointUrl(url);
        form.setUsername(user);
        form.setSecret(secret);
        return form;
    }

    @Test
    @DisplayName("Valid https settings pass; the DTO has a trimmed URL and the typed credential")
    void validForm() {
        IntegrationSettingsForm form = form("  https://ehr.hospital.example:8443/fhir ", "svc", "Abcdefg-123");
        form.setExpectedVersion(3);

        assertThat(form.validate(false)).isEmpty();
        IntegrationSettingsDTO dto = form.toSettings();
        assertThat(dto.endpointUrl()).isEqualTo("https://ehr.hospital.example:8443/fhir");
        assertThat(dto.secret()).isEqualTo("Abcdefg-123");
        assertThat(form.getExpectedVersion()).isEqualTo(3);
        assertThat(form.getUsername()).isEqualTo("svc");
    }

    @Test
    @DisplayName("Endpoint rules: required, https, host, no credentials in the URL, port, syntax, length")
    void endpointRules() {
        assertThat(form(null, null, "Abcdefg-123").validate(false)).containsKey("endpointUrl");
        assertThat(form("  ", null, "Abcdefg-123").validate(false).get("endpointUrl")).contains("required");
        assertThat(form("http://plain.example", null, "Abcdefg-123").validate(false).get("endpointUrl")).contains("https");
        assertThat(form("https:///only-a-path", null, "Abcdefg-123").validate(false).get("endpointUrl")).contains("host");
        assertThat(form("https://u:p@h.example", null, "Abcdefg-123").validate(false).get("endpointUrl")).contains("credential field");
        assertThat(form("https://h.example:70000", null, "Abcdefg-123").validate(false).get("endpointUrl")).contains("port");
        assertThat(form("https://bad host", null, "Abcdefg-123").validate(false).get("endpointUrl")).contains("not a valid URL");
        assertThat(form("https://h.example/" + "a".repeat(2100), null, "Abcdefg-123").validate(false).get("endpointUrl")).contains("2048");
    }

    @Test
    @DisplayName("User name is optional but limited")
    void usernameRules() {
        assertThat(form("https://h.example", "", "Abcdefg-123").validate(false)).isEmpty();
        assertThat(form("https://h.example", "u".repeat(101), "Abcdefg-123").validate(false)).containsKey("username");
        assertThat(form("https://h.example", "bad\nname", "Abcdefg-123").validate(false)).containsKey("username");
    }

    @Test
    @DisplayName("Credential is required the first time, optional later, and limited in length")
    void secretRules() {
        assertThat(form("https://h.example", null, null).validate(false).get("secret")).contains("required");
        assertThat(form("https://h.example", null, " ").validate(true)).isEmpty();
        assertThat(form("https://h.example", null, "short").validate(true).get("secret")).contains("between 8 and 256");
        assertThat(form("https://h.example", null, "x".repeat(257)).validate(true)).containsKey("secret");
    }

    @Test
    @DisplayName("A4: toString masks the credential, error messages do not contain it and clearSecret removes it")
    void secretIsNeverExposed() {
        IntegrationSettingsForm form = form("http://x", null, "tiny");
        Map<String, String> errors = form.validate(true);

        assertThat(form.toString()).doesNotContain("tiny").contains("****");
        assertThat(String.join(" ", errors.values())).doesNotContain("tiny");

        form.clearSecret();
        assertThat(form.getSecret()).isNull();
        assertThat(form.toString()).contains("<none>");
    }
}
