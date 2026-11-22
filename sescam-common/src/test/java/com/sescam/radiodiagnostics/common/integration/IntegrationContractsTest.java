package com.sescam.radiodiagnostics.common.integration;

import com.sescam.radiodiagnostics.common.security.AccessControlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationContractsTest {

    @Test
    @DisplayName("A4: the settings' toString masks the secret")
    void settingsMaskSecret() {
        String text = new IntegrationSettingsDTO("https://h.example", "svc", "S3cret-Value-123").toString();

        assertThat(text).doesNotContain("S3cret-Value-123").contains("****").contains("https://h.example");
        assertThat(new IntegrationSettingsDTO("https://h.example", "svc", null).toString()).contains("<none>");
        assertThat(new IntegrationSettingsDTO("https://h.example", "svc", " ").toString()).contains("<none>");
    }

    @Test
    @DisplayName("Only CONNECTION_FAILED and UNAVAILABLE are failures")
    void statusFailures() {
        for (IntegrationStatus status : IntegrationStatus.values()) {
            boolean expected = status == IntegrationStatus.CONNECTION_FAILED || status == IntegrationStatus.UNAVAILABLE;
            assertThat(status.isFailure()).isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("SettingsCheckResultDTO: canSave only for valid settings without a failed check")
    void settingsCheckResult() {
        ConnectionCheckResultDTO ok = new ConnectionCheckResultDTO("sso", IntegrationStatus.CONNECTED, "ok", Instant.EPOCH);
        ConnectionCheckResultDTO notChecked = new ConnectionCheckResultDTO("sso", IntegrationStatus.NOT_CHECKED, "n", Instant.EPOCH);
        ConnectionCheckResultDTO down = new ConnectionCheckResultDTO("sso", IntegrationStatus.UNAVAILABLE, "d", Instant.EPOCH);

        assertThat(SettingsCheckResultDTO.checked("sso", ok).canSave()).isTrue();
        assertThat(SettingsCheckResultDTO.checked("sso", notChecked).canSave()).isTrue();
        assertThat(SettingsCheckResultDTO.checked("sso", down).canSave()).isFalse();
        assertThat(SettingsCheckResultDTO.invalid("sso", List.of("bad")).canSave()).isFalse();
        assertThat(new SettingsCheckResultDTO("sso", true, List.of(), null).canSave()).isTrue();
    }

    @Test
    @DisplayName("Exceptions carry their data and messages without secrets")
    void exceptions() {
        assertThat(new IntegrationNotFoundException("x").getIntegrationId()).isEqualTo("x");
        assertThat(new UnauthorizedIntegrationAccessException("u", "no").getActorId()).isEqualTo("u");
        InvalidIntegrationSettingsException invalid = new InvalidIntegrationSettingsException("sso", List.of("a.", "b."));
        assertThat(invalid.getIntegrationId()).isEqualTo("sso");
        assertThat(invalid.getErrors()).containsExactly("a.", "b.");
        IntegrationConnectionFailedException failed =
                new IntegrationConnectionFailedException("sso", IntegrationStatus.UNAVAILABLE, "down");
        assertThat(failed.getIntegrationId()).isEqualTo("sso");
        assertThat(failed.getStatus()).isEqualTo(IntegrationStatus.UNAVAILABLE);
        ConcurrentIntegrationChangeException concurrent = new ConcurrentIntegrationChangeException("sso", 1, 2);
        assertThat(concurrent.getIntegrationId()).isEqualTo("sso");
        assertThat(concurrent.getExpectedVersion()).isEqualTo(1);
        assertThat(concurrent.getActualVersion()).isEqualTo(2);
        IntegrationSaveException save = new IntegrationSaveException("sso", new RuntimeException("secret-detail"));
        assertThat(save.getIntegrationId()).isEqualTo("sso");
        assertThat(save.getMessage()).doesNotContain("secret-detail");
    }

    @Test
    @DisplayName("canManageIntegrations falls back to canManageConfiguration until C1 defines its own permission")
    void accessControlDefaultMethod() {
        AccessControlService onlyConfig = (actorId, role) -> "ADMIN".equals(role);

        assertThat(onlyConfig.canManageIntegrations("a", "ADMIN")).isTrue();
        assertThat(onlyConfig.canManageIntegrations("a", "PATIENT")).isFalse();
    }
}
