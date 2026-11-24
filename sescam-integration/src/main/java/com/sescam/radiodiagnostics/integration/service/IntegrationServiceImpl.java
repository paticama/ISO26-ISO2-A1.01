package com.sescam.radiodiagnostics.integration.service;

import com.sescam.radiodiagnostics.common.integration.ConcurrentIntegrationChangeException;
import com.sescam.radiodiagnostics.common.integration.ConnectionCheckResultDTO;
import com.sescam.radiodiagnostics.common.integration.IntegrationConnectionFailedException;
import com.sescam.radiodiagnostics.common.integration.IntegrationNotFoundException;
import com.sescam.radiodiagnostics.common.integration.IntegrationSaveException;
import com.sescam.radiodiagnostics.common.integration.IntegrationService;
import com.sescam.radiodiagnostics.common.integration.IntegrationSettingsDTO;
import com.sescam.radiodiagnostics.common.integration.IntegrationStatus;
import com.sescam.radiodiagnostics.common.integration.IntegrationSummaryDTO;
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
import com.sescam.radiodiagnostics.integration.repository.IntegrationRepository;
import com.sescam.radiodiagnostics.integration.secret.SecretStore;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Core service of C9 (sescam-integration) realizing UC-ADM-04: Manage External Integrations and
 * Interoperability.
 * <p>
 * Secrets: the credential only travels in {@link IntegrationSettingsDTO}, is kept in the {@link SecretStore}
 * and is never copied to results, exceptions or audit events (A4).
 */
public class IntegrationServiceImpl implements IntegrationService {

    static final String ACTION_UPDATED = "INTEGRATION_CONFIGURATION_UPDATED";
    static final String ACTION_REJECTED = "INTEGRATION_CONFIGURATION_REJECTED";
    static final String ACTION_CHECKED = "INTEGRATION_CONNECTION_CHECKED";
    static final String ACTION_DENIED = "INTEGRATION_ACCESS_DENIED";

    private final IntegrationRepository repository;
    private final SecretStore secretStore;
    private final ConnectionProbe probe;
    private final SettingsValidator validator;
    private final AccessControlService accessControlService;
    private final AuditService auditService;
    private final Clock clock;

    public IntegrationServiceImpl(IntegrationRepository repository, SecretStore secretStore,
                                  ConnectionProbe probe, AccessControlService accessControlService,
                                  AuditService auditService) {
        this(repository, secretStore, probe, new SettingsValidator(), accessControlService, auditService,
                Clock.systemUTC());
    }

    public IntegrationServiceImpl(IntegrationRepository repository, SecretStore secretStore,
                                  ConnectionProbe probe, SettingsValidator validator,
                                  AccessControlService accessControlService, AuditService auditService,
                                  Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.secretStore = Objects.requireNonNull(secretStore, "secretStore must not be null");
        this.probe = Objects.requireNonNull(probe, "probe must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
        this.accessControlService = Objects.requireNonNull(accessControlService, "accessControlService must not be null");
        this.auditService = Objects.requireNonNull(auditService, "auditService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public List<IntegrationSummaryDTO> listIntegrations(String administratorId, String administratorRole) {
        ensureAuthorized(administratorId, administratorRole, "integrations");
        return repository.findAll().stream().map(Integration::toDTO).toList();
    }

    @Override
    public IntegrationSummaryDTO getIntegration(String integrationId, String administratorId,
                                                String administratorRole) {
        ensureAuthorized(administratorId, administratorRole, integrationId);
        return findOrThrow(integrationId).toDTO();
    }

    @Override
    public SettingsCheckResultDTO validateSettings(String integrationId, IntegrationSettingsDTO settings,
                                                   String administratorId, String administratorRole) {
        ensureAuthorized(administratorId, administratorRole, integrationId);
        Integration existing = findOrThrow(integrationId);

        List<String> errors = validator.validate(settings, existing.isCredentialConfigured());
        if (!errors.isEmpty()) {
            return SettingsCheckResultDTO.invalid(integrationId, errors);
        }
        return SettingsCheckResultDTO.checked(integrationId, check(existing, settings));
    }

    @Override
    public UpdateIntegrationResponseDTO updateIntegration(UpdateIntegrationRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");
        String adminId = request.administratorId();
        String id = request.integrationId();

        // A1. Unauthorized administrator: nothing is read or changed
        ensureAuthorized(adminId, request.administratorRole(), id);

        Integration existing = findOrThrow(id);

        if (existing.getVersion() != request.expectedVersion()) {
            throw new ConcurrentIntegrationChangeException(id, request.expectedVersion(), existing.getVersion());
        }

        // A2. Invalid settings: the working configuration is kept
        IntegrationSettingsDTO settings = request.settings();
        List<String> errors = validator.validate(settings, existing.isCredentialConfigured());
        if (!errors.isEmpty()) {
            audit(ACTION_REJECTED, adminId, id, existing.getEndpointUrl(), null,
                    "Invalid settings: " + String.join(" ", errors));
            throw new InvalidIntegrationSettingsException(id, errors);
        }

        // Step 5: connection check with the proposed settings. A2/A3: a failure does not replace the working one
        ConnectionCheckResultDTO connection = check(existing, settings);
        if (connection.status().isFailure()) {
            audit(ACTION_REJECTED, adminId, id, existing.getEndpointUrl(), null,
                    "Connection check failed: " + connection.status());
            throw new IntegrationConnectionFailedException(id, connection.status(), connection.message());
        }

        // Step 7: save; if anything fails, the previous configuration and credential stay active
        Instant now = Instant.now(clock);
        boolean newSecret = settings.secret() != null && !settings.secret().isBlank();
        Integration updated = existing.withSettings(settings.endpointUrl().trim(), blankToNull(settings.username()),
                existing.isCredentialConfigured() || newSecret, connection.status(), connection.message(),
                adminId, now);
        save(existing, updated, newSecret ? settings.secret() : null);

        // <<include>> Record Audit Event: never contains the credential
        audit(ACTION_UPDATED, adminId, id, existing.getEndpointUrl(), updated.getEndpointUrl(),
                "status=" + updated.getStatus() + "; version=" + updated.getVersion()
                        + "; credentialChanged=" + newSecret);

        return new UpdateIntegrationResponseDTO(id, updated.getStatus(), updated.getStatusMessage(),
                updated.getVersion(), adminId, now);
    }

    @Override
    public ConnectionCheckResultDTO checkConnection(String integrationId, String administratorId,
                                                    String administratorRole) {
        ensureAuthorized(administratorId, administratorRole, integrationId);
        Integration existing = findOrThrow(integrationId);

        if (!existing.isConfigured()) {
            return new ConnectionCheckResultDTO(integrationId, IntegrationStatus.NOT_CONFIGURED,
                    "Not configured yet.", Instant.now(clock));
        }

        IntegrationSettingsDTO saved = new IntegrationSettingsDTO(existing.getEndpointUrl(), existing.getUsername(), null);
        ConnectionCheckResultDTO result = check(existing, saved);

        // A3: the status reflects the condition; the settings and the version do not change
        repository.save(existing.withStatus(result.status(), result.message(), result.checkedAt()));
        audit(ACTION_CHECKED, administratorId, integrationId, null, null, "status=" + result.status());
        return result;
    }

    /** Runs the probe with the typed credential, or with the stored one when none was typed. */
    private ConnectionCheckResultDTO check(Integration existing, IntegrationSettingsDTO settings) {
        String secret = settings.secret() != null && !settings.secret().isBlank()
                ? settings.secret()
                : secretStore.get(existing.getId()).orElse(null);
        ProbeResult result;
        try {
            result = probe.probe(existing.getType(), settings.endpointUrl().trim(), settings.username(), secret);
        } catch (RuntimeException ex) {
            // the original message could contain the URL or the credential, so it is not copied
            result = ProbeResult.unreachable("The connection check could not be completed.");
        }
        return new ConnectionCheckResultDTO(existing.getId(), result.status(), result.message(), Instant.now(clock));
    }

    private void save(Integration previous, Integration updated, String newSecret) {
        try {
            repository.save(updated);
            if (newSecret != null) {
                secretStore.put(updated.getId(), newSecret);
            }
        } catch (RuntimeException ex) {
            restore(previous);
            throw new IntegrationSaveException(updated.getId(), ex);
        }
    }

    private void restore(Integration previous) {
        try {
            repository.save(previous);
        } catch (RuntimeException ignored) {
            // nothing more can be done; the caller reports the save failure
        }
    }

    private void ensureAuthorized(String actorId, String role, String resource) {
        if (!accessControlService.canManageIntegrations(actorId, role)) {
            try {
                audit(ACTION_DENIED, actorId, resource, null, null, "role=" + role);
            } catch (RuntimeException ignored) {
                // a failure of the audit trail must not hide the denial
            }
            throw new UnauthorizedIntegrationAccessException(actorId,
                    "Access denied: user '" + actorId + "' is not authorized to manage integrations.");
        }
    }

    private Integration findOrThrow(String integrationId) {
        return repository.findById(integrationId).orElseThrow(() -> new IntegrationNotFoundException(integrationId));
    }

    private void audit(String action, String actorId, String resourceKey, String previousValue, String newValue,
                       String details) {
        String actor = actorId == null || actorId.isBlank() ? "unknown" : actorId;
        auditService.recordEvent(new AuditEventDTO(action, actor, resourceKey, previousValue, newValue, details,
                Instant.now(clock)));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
