package com.sescam.radiodiagnostics.integration.domain;

import com.sescam.radiodiagnostics.common.integration.IntegrationStatus;
import com.sescam.radiodiagnostics.common.integration.IntegrationSummaryDTO;
import com.sescam.radiodiagnostics.common.integration.IntegrationType;
import com.sescam.radiodiagnostics.integration.connection.ConnectionProbe;
import com.sescam.radiodiagnostics.integration.connection.NoOpConnectionProbe;
import com.sescam.radiodiagnostics.integration.repository.InMemoryIntegrationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrationAndRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-11-21T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-11-21T10:00:00Z");

    @Test
    @DisplayName("A new integration is not configured, version 1, and has no credential")
    void notConfigured() {
        Integration integration = Integration.notConfigured("sso", IntegrationType.SSO, "SSO", T0);

        assertThat(integration.isConfigured()).isFalse();
        assertThat(integration.getStatus()).isEqualTo(IntegrationStatus.NOT_CONFIGURED);
        assertThat(integration.getVersion()).isEqualTo(1L);
        assertThat(integration.isCredentialConfigured()).isFalse();
    }

    @Test
    @DisplayName("withSettings creates a new version; withStatus keeps settings and version; the original is unchanged")
    void immutableUpdates() {
        Integration original = Integration.notConfigured("sso", IntegrationType.SSO, "SSO", T0);

        Integration saved = original.withSettings("https://h.example", "svc", true, IntegrationStatus.CONNECTED,
                "ok", "admin-1", T1);
        Integration refreshed = saved.withStatus(IntegrationStatus.UNAVAILABLE, "down", T1.plusSeconds(60));

        assertThat(original.getVersion()).isEqualTo(1L);
        assertThat(saved.getVersion()).isEqualTo(2L);
        assertThat(saved.isConfigured()).isTrue();
        assertThat(saved.getLastModifiedBy()).isEqualTo("admin-1");
        assertThat(refreshed.getVersion()).isEqualTo(2L);
        assertThat(refreshed.getEndpointUrl()).isEqualTo("https://h.example");
        assertThat(refreshed.getStatus()).isEqualTo(IntegrationStatus.UNAVAILABLE);
        assertThat(refreshed.getLastCheckedAt()).isEqualTo(T1.plusSeconds(60));
        assertThat(refreshed.getLastModifiedAt()).isEqualTo(T1);
        assertThat(refreshed.getDisplayName()).isEqualTo("SSO");
        assertThat(refreshed.getType()).isEqualTo(IntegrationType.SSO);
        assertThat(refreshed.getUsername()).isEqualTo("svc");
        assertThat(refreshed.getStatusMessage()).isEqualTo("down");
    }

    @Test
    @DisplayName("The DTO of an integration has the credential flag but nothing secret")
    void dto() {
        Integration saved = Integration.notConfigured("sso", IntegrationType.SSO, "SSO", T0)
                .withSettings("https://h.example", "svc", true, IntegrationStatus.CONNECTED, "ok", "admin-1", T1);

        IntegrationSummaryDTO dto = saved.toDTO();

        assertThat(dto.credentialConfigured()).isTrue();
        assertThat(dto.endpointUrl()).isEqualTo("https://h.example");
        assertThat(dto.version()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Required fields are checked")
    void requiredFields() {
        assertThatThrownBy(() -> Integration.notConfigured(null, IntegrationType.SSO, "SSO", T0))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Repository is seeded with the four systems, sorted, and saves replace by id")
    void repository() {
        InMemoryIntegrationRepository repository = new InMemoryIntegrationRepository();

        assertThat(repository.findAll()).extracting(Integration::getId).containsExactly("ehr", "messaging", "ris-pacs", "sso");
        assertThat(repository.findById("nope")).isEmpty();
        assertThat(repository.findById(null)).isEmpty();

        Integration sso = repository.findById("sso").orElseThrow();
        repository.save(sso.withSettings("https://h.example", null, true, IntegrationStatus.NOT_CHECKED, "m", "a", T1));
        assertThat(repository.findById("sso").orElseThrow().getVersion()).isEqualTo(2L);
        assertThat(repository.findAll()).hasSize(4);
    }

    @Test
    @DisplayName("The no-op probe never claims a connection")
    void noOpProbe() {
        ConnectionProbe.ProbeResult result = new NoOpConnectionProbe()
                .probe(IntegrationType.EHR, "https://h.example", "u", "Secret-1234");

        assertThat(result.status()).isEqualTo(IntegrationStatus.NOT_CHECKED);
        assertThat(result.message()).doesNotContain("Secret-1234");
        assertThat(ConnectionProbe.ProbeResult.connected("x").status()).isEqualTo(IntegrationStatus.CONNECTED);
        assertThat(ConnectionProbe.ProbeResult.refused("x").status()).isEqualTo(IntegrationStatus.CONNECTION_FAILED);
        assertThat(ConnectionProbe.ProbeResult.unreachable("x").status()).isEqualTo(IntegrationStatus.UNAVAILABLE);
    }
}
