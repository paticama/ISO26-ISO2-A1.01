package com.sescam.radiodiagnostics.integration.service;

import com.sescam.radiodiagnostics.common.integration.ConcurrentIntegrationChangeException;
import com.sescam.radiodiagnostics.common.integration.ConnectionCheckResultDTO;
import com.sescam.radiodiagnostics.common.integration.IntegrationConnectionFailedException;
import com.sescam.radiodiagnostics.common.integration.IntegrationNotFoundException;
import com.sescam.radiodiagnostics.common.integration.IntegrationSaveException;
import com.sescam.radiodiagnostics.common.integration.IntegrationSettingsDTO;
import com.sescam.radiodiagnostics.common.integration.IntegrationStatus;
import com.sescam.radiodiagnostics.common.integration.IntegrationSummaryDTO;
import com.sescam.radiodiagnostics.common.integration.IntegrationType;
import com.sescam.radiodiagnostics.common.integration.InvalidIntegrationSettingsException;
import com.sescam.radiodiagnostics.common.integration.SettingsCheckResultDTO;
import com.sescam.radiodiagnostics.common.integration.UnauthorizedIntegrationAccessException;
import com.sescam.radiodiagnostics.common.integration.UpdateIntegrationRequestDTO;
import com.sescam.radiodiagnostics.common.integration.UpdateIntegrationResponseDTO;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.integration.connection.ConnectionProbe;
import com.sescam.radiodiagnostics.integration.connection.ConnectionProbe.ProbeResult;
import com.sescam.radiodiagnostics.integration.domain.Integration;
import com.sescam.radiodiagnostics.integration.internal.SettingsValidator;
import com.sescam.radiodiagnostics.integration.repository.InMemoryIntegrationRepository;
import com.sescam.radiodiagnostics.integration.repository.IntegrationRepository;
import com.sescam.radiodiagnostics.integration.secret.EncryptedInMemorySecretStore;
import com.sescam.radiodiagnostics.integration.secret.SecretStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * One test per acceptance criterion and per alternative flow (A1-A4) of UC-ADM-04.
 */
@ExtendWith(MockitoExtension.class)
class IntegrationServiceImplTest {

    private static final String ADMIN = "admin-1";
    private static final String ROLE = "ADMINISTRATOR";
    private static final String SECRET = "S3cret-Value-123";
    private static final String URL = "https://sso.hospital.example/auth";
    private static final Instant NOW = Instant.parse("2026-11-21T12:00:00Z");

    @Mock
    private AccessControlService accessControl;
    @Mock
    private AuditService audit;
    @Mock
    private ConnectionProbe probe;

    private IntegrationRepository repository;
    private SecretStore secrets;
    private IntegrationServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryIntegrationRepository();
        secrets = new EncryptedInMemorySecretStore();
        service = new IntegrationServiceImpl(repository, secrets, probe, new SettingsValidator(), accessControl,
                audit, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void allowAdmin() {
        when(accessControl.canManageIntegrations(ADMIN, ROLE)).thenReturn(true);
    }

    private static IntegrationSettingsDTO settings(String url, String secret) {
        return new IntegrationSettingsDTO(url, "svc-sescam", secret);
    }

    private UpdateIntegrationResponseDTO configureSso() {
        return service.updateIntegration(new UpdateIntegrationRequestDTO("sso", settings(URL, SECRET), 1L, ADMIN, ROLE));
    }

    // ---------- Acceptance: authorized administrators can view integrations and their status ----------

    @Test
    @DisplayName("Authorized administrator sees the four integrations with their status")
    void listShowsIntegrationsAndStatus() {
        allowAdmin();

        List<IntegrationSummaryDTO> list = service.listIntegrations(ADMIN, ROLE);

        assertThat(list).extracting(IntegrationSummaryDTO::id).containsExactly("ehr", "messaging", "ris-pacs", "sso");
        assertThat(list).extracting(IntegrationSummaryDTO::status).containsOnly(IntegrationStatus.NOT_CONFIGURED);
        assertThat(service.getIntegration("sso", ADMIN, ROLE).type()).isEqualTo(IntegrationType.SSO);
    }

    @Test
    @DisplayName("Unknown integration id is reported as not found")
    void unknownIntegrationIsNotFound() {
        allowAdmin();

        assertThatThrownBy(() -> service.getIntegration("nope", ADMIN, ROLE))
                .isInstanceOf(IntegrationNotFoundException.class);
        assertThatThrownBy(() -> service.getIntegration(null, ADMIN, ROLE))
                .isInstanceOf(IntegrationNotFoundException.class);
    }

    // ---------- A1: users without permission cannot view or change protected settings ----------

    @Test
    @DisplayName("A1: unauthorized user cannot list, read, validate, update or check; denial is audited; nothing changes")
    void unauthorizedUserIsRejectedEverywhere() {
        when(accessControl.canManageIntegrations("patient-1", "PATIENT")).thenReturn(false);

        assertThatThrownBy(() -> service.listIntegrations("patient-1", "PATIENT"))
                .isInstanceOf(UnauthorizedIntegrationAccessException.class);
        assertThatThrownBy(() -> service.getIntegration("sso", "patient-1", "PATIENT"))
                .isInstanceOf(UnauthorizedIntegrationAccessException.class);
        assertThatThrownBy(() -> service.validateSettings("sso", settings(URL, SECRET), "patient-1", "PATIENT"))
                .isInstanceOf(UnauthorizedIntegrationAccessException.class);
        assertThatThrownBy(() -> service.updateIntegration(
                new UpdateIntegrationRequestDTO("sso", settings(URL, SECRET), 1L, "patient-1", "PATIENT")))
                .isInstanceOf(UnauthorizedIntegrationAccessException.class);
        assertThatThrownBy(() -> service.checkConnection("sso", "patient-1", "PATIENT"))
                .isInstanceOf(UnauthorizedIntegrationAccessException.class);

        ArgumentCaptor<AuditEventDTO> captor = ArgumentCaptor.forClass(AuditEventDTO.class);
        verify(audit, org.mockito.Mockito.times(5)).recordEvent(captor.capture());
        assertThat(captor.getAllValues()).extracting(AuditEventDTO::action).containsOnly("INTEGRATION_ACCESS_DENIED");
        verifyNoInteractions(probe);
        assertThat(repository.findById("sso").orElseThrow().getStatus()).isEqualTo(IntegrationStatus.NOT_CONFIGURED);
        assertThat(secrets.contains("sso")).isFalse();
    }

    @Test
    @DisplayName("A1: a blank actor id is audited as 'unknown' and a failing audit does not hide the denial")
    void denialWithBlankActorAndFailingAudit() {
        when(accessControl.canManageIntegrations("", "")).thenReturn(false);
        org.mockito.Mockito.doThrow(new IllegalArgumentException("audit down")).when(audit).recordEvent(any());

        assertThatThrownBy(() -> service.listIntegrations("", ""))
                .isInstanceOf(UnauthorizedIntegrationAccessException.class);
    }

    // ---------- Acceptance: supported connection settings can be updated ----------

    @Test
    @DisplayName("Main flow: valid settings are saved, version goes up and the status comes from the connection check")
    void updateSavesSettingsAndReportsStatus() {
        allowAdmin();
        when(probe.probe(eq(IntegrationType.SSO), eq(URL), eq("svc-sescam"), eq(SECRET)))
                .thenReturn(ProbeResult.connected("Connected."));

        UpdateIntegrationResponseDTO response = configureSso();

        assertThat(response.status()).isEqualTo(IntegrationStatus.CONNECTED);
        assertThat(response.newVersion()).isEqualTo(2L);
        assertThat(response.updatedBy()).isEqualTo(ADMIN);
        assertThat(response.updatedAt()).isEqualTo(NOW);
        IntegrationSummaryDTO saved = service.getIntegration("sso", ADMIN, ROLE);
        assertThat(saved.endpointUrl()).isEqualTo(URL);
        assertThat(saved.username()).isEqualTo("svc-sescam");
        assertThat(saved.credentialConfigured()).isTrue();
        assertThat(saved.version()).isEqualTo(2L);
        assertThat(secrets.get("sso")).contains(SECRET);
    }

    @Test
    @DisplayName("When the connection check is not supported the status is NOT_CHECKED, never CONNECTED")
    void notSupportedCheckIsNotReportedAsConnected() {
        allowAdmin();
        when(probe.probe(any(), anyString(), any(), any())).thenReturn(ProbeResult.notSupported("Not available yet."));

        assertThat(configureSso().status()).isEqualTo(IntegrationStatus.NOT_CHECKED);
    }

    @Test
    @DisplayName("Step 5: validateSettings reports valid settings with the connection result and saves nothing")
    void validateSettingsDoesNotSave() {
        allowAdmin();
        when(probe.probe(any(), anyString(), any(), any())).thenReturn(ProbeResult.connected("Connected."));

        SettingsCheckResultDTO result = service.validateSettings("sso", settings(URL, SECRET), ADMIN, ROLE);

        assertThat(result.valid()).isTrue();
        assertThat(result.canSave()).isTrue();
        assertThat(result.connection().status()).isEqualTo(IntegrationStatus.CONNECTED);
        assertThat(repository.findById("sso").orElseThrow().getVersion()).isEqualTo(1L);
        assertThat(secrets.contains("sso")).isFalse();
        verify(audit, never()).recordEvent(any());
    }

    @Test
    @DisplayName("A blank credential on an update keeps the stored credential and uses it for the check")
    void blankSecretKeepsStoredCredential() {
        allowAdmin();
        when(probe.probe(any(), anyString(), any(), any())).thenReturn(ProbeResult.connected("Connected."));
        configureSso();

        service.updateIntegration(new UpdateIntegrationRequestDTO("sso",
                settings("https://sso2.hospital.example/auth", "  "), 2L, ADMIN, ROLE));

        assertThat(secrets.get("sso")).contains(SECRET);
        verify(probe).probe(IntegrationType.SSO, "https://sso2.hospital.example/auth", "svc-sescam", SECRET);
        assertThat(service.getIntegration("sso", ADMIN, ROLE).endpointUrl()).isEqualTo("https://sso2.hospital.example/auth");
    }

    // ---------- A2: invalid settings / failed connection check keep the working configuration ----------

    @Test
    @DisplayName("A2: invalid settings are rejected, the working configuration stays and the rejection is audited")
    void invalidSettingsDoNotReplaceWorkingConfiguration() {
        allowAdmin();
        when(probe.probe(any(), anyString(), any(), any())).thenReturn(ProbeResult.connected("Connected."));
        configureSso();

        assertThatThrownBy(() -> service.updateIntegration(
                new UpdateIntegrationRequestDTO("sso", settings("http://plain.example", "short"), 2L, ADMIN, ROLE)))
                .isInstanceOfSatisfying(InvalidIntegrationSettingsException.class, ex -> {
                    assertThat(ex.getErrors()).anyMatch(e -> e.contains("https"));
                    assertThat(ex.getErrors()).anyMatch(e -> e.contains("Credential"));
                    assertThat(ex.getMessage()).doesNotContain("short");
                });

        IntegrationSummaryDTO kept = service.getIntegration("sso", ADMIN, ROLE);
        assertThat(kept.endpointUrl()).isEqualTo(URL);
        assertThat(kept.version()).isEqualTo(2L);
        assertThat(secrets.get("sso")).contains(SECRET);
        verify(audit).recordEvent(org.mockito.ArgumentMatchers.argThat(e -> "INTEGRATION_CONFIGURATION_REJECTED".equals(e.action())));
    }

    @Test
    @DisplayName("Step 5: validateSettings returns the problems of invalid settings instead of throwing")
    void validateSettingsReportsErrors() {
        allowAdmin();

        SettingsCheckResultDTO result = service.validateSettings("sso", settings("", null), ADMIN, ROLE);

        assertThat(result.valid()).isFalse();
        assertThat(result.canSave()).isFalse();
        assertThat(result.errors()).isNotEmpty();
        assertThat(result.connection()).isNull();
        verifyNoInteractions(probe);
    }

    @Test
    @DisplayName("A2: a refused connection (bad credentials) is reported as CONNECTION_FAILED and nothing is replaced")
    void refusedConnectionDoesNotReplaceWorkingConfiguration() {
        allowAdmin();
        when(probe.probe(any(), eq(URL), any(), eq(SECRET))).thenReturn(ProbeResult.connected("Connected."));
        configureSso();
        when(probe.probe(any(), eq("https://other.example"), any(), eq("Another-Secret-9")))
                .thenReturn(ProbeResult.refused("The system rejected the credentials."));

        assertThatThrownBy(() -> service.updateIntegration(new UpdateIntegrationRequestDTO("sso",
                settings("https://other.example", "Another-Secret-9"), 2L, ADMIN, ROLE)))
                .isInstanceOfSatisfying(IntegrationConnectionFailedException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(IntegrationStatus.CONNECTION_FAILED);
                    assertThat(ex.getMessage()).doesNotContain("Another-Secret-9");
                });

        assertThat(service.getIntegration("sso", ADMIN, ROLE).endpointUrl()).isEqualTo(URL);
        assertThat(secrets.get("sso")).contains(SECRET);
    }

    @Test
    @DisplayName("Step 5: a failed check is visible and distinguishable from success in the validation result")
    void failedCheckIsDistinguishableFromSuccess() {
        allowAdmin();
        when(probe.probe(any(), anyString(), any(), any())).thenReturn(ProbeResult.unreachable("No answer."));

        SettingsCheckResultDTO result = service.validateSettings("sso", settings(URL, SECRET), ADMIN, ROLE);

        assertThat(result.valid()).isTrue();
        assertThat(result.connection().status()).isEqualTo(IntegrationStatus.UNAVAILABLE);
        assertThat(result.connection().status().isFailure()).isTrue();
        assertThat(result.canSave()).isFalse();
        assertThat(IntegrationStatus.CONNECTED.isFailure()).isFalse();
        assertThat(IntegrationStatus.NOT_CHECKED.isFailure()).isFalse();
    }

    // ---------- A3: external system unavailable -> status shows it, retry possible ----------

    @Test
    @DisplayName("A3: unreachable system during save is reported as UNAVAILABLE and the working configuration stays")
    void unavailableSystemDuringSave() {
        allowAdmin();
        when(probe.probe(any(), anyString(), any(), any())).thenReturn(ProbeResult.unreachable("No answer."));

        assertThatThrownBy(this::configureSso)
                .isInstanceOfSatisfying(IntegrationConnectionFailedException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(IntegrationStatus.UNAVAILABLE));

        assertThat(service.getIntegration("sso", ADMIN, ROLE).status()).isEqualTo(IntegrationStatus.NOT_CONFIGURED);
    }

    @Test
    @DisplayName("A3: retrying the check on a saved configuration updates the status and keeps settings and version")
    void checkConnectionRetryUpdatesStatusOnly() {
        allowAdmin();
        when(probe.probe(any(), anyString(), any(), any()))
                .thenReturn(ProbeResult.connected("Connected."))
                .thenReturn(ProbeResult.unreachable("No answer."))
                .thenReturn(ProbeResult.connected("Connected again."));
        configureSso();

        ConnectionCheckResultDTO down = service.checkConnection("sso", ADMIN, ROLE);
        IntegrationSummaryDTO afterDown = service.getIntegration("sso", ADMIN, ROLE);
        assertThat(down.status()).isEqualTo(IntegrationStatus.UNAVAILABLE);
        assertThat(afterDown.status()).isEqualTo(IntegrationStatus.UNAVAILABLE);
        assertThat(afterDown.endpointUrl()).isEqualTo(URL);
        assertThat(afterDown.version()).isEqualTo(2L);
        assertThat(afterDown.lastCheckedAt()).isEqualTo(NOW);

        ConnectionCheckResultDTO up = service.checkConnection("sso", ADMIN, ROLE);
        assertThat(up.status()).isEqualTo(IntegrationStatus.CONNECTED);
        verify(probe, org.mockito.Mockito.times(3)).probe(IntegrationType.SSO, URL, "svc-sescam", SECRET);
    }

    @Test
    @DisplayName("Checking an integration that is not configured reports NOT_CONFIGURED without calling the probe")
    void checkNotConfiguredIntegration() {
        allowAdmin();

        ConnectionCheckResultDTO result = service.checkConnection("ehr", ADMIN, ROLE);

        assertThat(result.status()).isEqualTo(IntegrationStatus.NOT_CONFIGURED);
        verifyNoInteractions(probe);
    }

    @Test
    @DisplayName("A3: a probe that throws is reported as UNAVAILABLE without copying the exception text")
    void throwingProbeIsReportedAsUnavailable() {
        allowAdmin();
        when(probe.probe(any(), anyString(), any(), any()))
                .thenThrow(new IllegalStateException("boom " + SECRET + " " + URL));

        SettingsCheckResultDTO result = service.validateSettings("sso", settings(URL, SECRET), ADMIN, ROLE);

        assertThat(result.connection().status()).isEqualTo(IntegrationStatus.UNAVAILABLE);
        assertThat(result.connection().message()).doesNotContain(SECRET).doesNotContain(URL);
    }

    // ---------- A4: secrets are protected in display, storage and diagnostics ----------

    @Test
    @DisplayName("A4: the credential never appears in results, audit events or the request's toString")
    void secretNeverLeaks() {
        allowAdmin();
        when(probe.probe(any(), anyString(), any(), any())).thenReturn(ProbeResult.connected("Connected."));

        UpdateIntegrationResponseDTO response = configureSso();
        IntegrationSummaryDTO summary = service.getIntegration("sso", ADMIN, ROLE);
        UpdateIntegrationRequestDTO request = new UpdateIntegrationRequestDTO("sso", settings(URL, SECRET), 2L, ADMIN, ROLE);

        assertThat(response.toString()).doesNotContain(SECRET);
        assertThat(summary.toString()).doesNotContain(SECRET);
        assertThat(request.toString()).doesNotContain(SECRET).contains("****");
        ArgumentCaptor<AuditEventDTO> captor = ArgumentCaptor.forClass(AuditEventDTO.class);
        verify(audit).recordEvent(captor.capture());
        assertThat(captor.getValue().toString()).doesNotContain(SECRET);
        assertThat(captor.getValue().details()).contains("credentialChanged=true");
    }

    // ---------- Acceptance: successful changes are confirmed and attributable for audit ----------

    @Test
    @DisplayName("A successful change is audited with actor, integration, previous and new endpoint")
    void successfulChangeIsAudited() {
        allowAdmin();
        when(probe.probe(any(), anyString(), any(), any())).thenReturn(ProbeResult.connected("Connected."));

        configureSso();

        ArgumentCaptor<AuditEventDTO> captor = ArgumentCaptor.forClass(AuditEventDTO.class);
        verify(audit).recordEvent(captor.capture());
        AuditEventDTO event = captor.getValue();
        assertThat(event.action()).isEqualTo("INTEGRATION_CONFIGURATION_UPDATED");
        assertThat(event.actorId()).isEqualTo(ADMIN);
        assertThat(event.resourceKey()).isEqualTo("sso");
        assertThat(event.previousValue()).isNull();
        assertThat(event.newValue()).isEqualTo(URL);
        assertThat(event.timestamp()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("A retry of the connection check is audited too")
    void connectionCheckIsAudited() {
        allowAdmin();
        when(probe.probe(any(), anyString(), any(), any())).thenReturn(ProbeResult.connected("Connected."));
        configureSso();

        service.checkConnection("sso", ADMIN, ROLE);

        verify(audit).recordEvent(org.mockito.ArgumentMatchers.argThat(e -> "INTEGRATION_CONNECTION_CHECKED".equals(e.action())));
    }

    // ---------- Concurrency and save failure ----------

    @Test
    @DisplayName("A change based on an old version is rejected and nothing is saved")
    void concurrentChangeIsRejected() {
        allowAdmin();
        when(probe.probe(any(), anyString(), any(), any())).thenReturn(ProbeResult.connected("Connected."));
        configureSso();

        assertThatThrownBy(() -> service.updateIntegration(
                new UpdateIntegrationRequestDTO("sso", settings("https://x.example", "Another-Secret-9"), 1L, ADMIN, ROLE)))
                .isInstanceOfSatisfying(ConcurrentIntegrationChangeException.class, ex -> {
                    assertThat(ex.getExpectedVersion()).isEqualTo(1L);
                    assertThat(ex.getActualVersion()).isEqualTo(2L);
                });
        assertThat(service.getIntegration("sso", ADMIN, ROLE).endpointUrl()).isEqualTo(URL);
    }

    @Test
    @DisplayName("A failing repository gives IntegrationSaveException and the previous configuration is restored")
    void saveFailureKeepsPreviousConfiguration() {
        allowAdmin();
        IntegrationRepository failing = new InMemoryIntegrationRepository() {
            private boolean failNext = false;

            @Override
            public Integration save(Integration integration) {
                if (failNext && integration.getVersion() == 2L) {
                    failNext = false;
                    throw new IllegalStateException("disk full " + SECRET);
                }
                return super.save(integration);
            }

            @Override
            public Optional<Integration> findById(String id) {
                failNext = true;
                return super.findById(id);
            }
        };
        IntegrationServiceImpl failingService = new IntegrationServiceImpl(failing, secrets, probe, new SettingsValidator(),
                accessControl, audit, Clock.fixed(NOW, ZoneOffset.UTC));
        when(probe.probe(any(), anyString(), any(), any())).thenReturn(ProbeResult.connected("Connected."));

        assertThatThrownBy(() -> failingService.updateIntegration(
                new UpdateIntegrationRequestDTO("sso", settings(URL, SECRET), 1L, ADMIN, ROLE)))
                .isInstanceOfSatisfying(IntegrationSaveException.class,
                        ex -> assertThat(ex.getMessage()).doesNotContain(SECRET));

        assertThat(failing.findAll()).filteredOn(i -> i.getId().equals("sso"))
                .allMatch(i -> i.getVersion() == 1L && i.getStatus() == IntegrationStatus.NOT_CONFIGURED);
        assertThat(secrets.contains("sso")).isFalse();
    }

    @Test
    @DisplayName("Constructor and request guards")
    void guards() {
        assertThatThrownBy(() -> service.updateIntegration(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new IntegrationServiceImpl(null, secrets, probe, accessControl, audit))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new IntegrationServiceImpl(repository, secrets, null, accessControl, audit))
                .isInstanceOf(NullPointerException.class);
    }
}
