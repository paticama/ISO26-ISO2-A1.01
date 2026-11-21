package com.sescam.radiodiagnostics.catalogue.service;

import com.sescam.radiodiagnostics.catalogue.domain.ConfigurationParameter;
import com.sescam.radiodiagnostics.catalogue.repository.ConfigurationParameterRepository;
import com.sescam.radiodiagnostics.common.catalogue.ActivationStatus;
import com.sescam.radiodiagnostics.common.catalogue.CatalogueService;
import com.sescam.radiodiagnostics.common.catalogue.ConcurrentConfigurationChangeException;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationNotFoundException;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationParameterDTO;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationSaveException;
import com.sescam.radiodiagnostics.common.catalogue.InvalidConfigurationValueException;
import com.sescam.radiodiagnostics.common.catalogue.UnauthorizedConfigurationAccessException;
import com.sescam.radiodiagnostics.common.catalogue.UpdateParameterRequestDTO;
import com.sescam.radiodiagnostics.common.catalogue.UpdateParameterResponseDTO;
import com.sescam.radiodiagnostics.common.catalogue.ValidationResultDTO;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Core service implementation for C2 (sescam-catalogue) realizing UC-ADM-01:
 * Manage System Configurations & Parameters.
 */
public class CatalogueServiceImpl implements CatalogueService {

    private final ConfigurationParameterRepository repository;
    private final AccessControlService accessControlService;
    private final AuditService auditService;
    private final Clock clock;

    public CatalogueServiceImpl(ConfigurationParameterRepository repository,
                                AccessControlService accessControlService,
                                AuditService auditService) {
        this(repository, accessControlService, auditService, Clock.systemUTC());
    }

    public CatalogueServiceImpl(ConfigurationParameterRepository repository,
                                AccessControlService accessControlService,
                                AuditService auditService,
                                Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.accessControlService = Objects.requireNonNull(accessControlService, "accessControlService must not be null");
        this.auditService = Objects.requireNonNull(auditService, "auditService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public List<ConfigurationParameterDTO> getAllParameters(String administratorId, String administratorRole) {
        ensureAuthorized(administratorId, administratorRole);
        return repository.findAll().stream()
                .map(ConfigurationParameter::toDTO)
                .toList();
    }

    @Override
    public ConfigurationParameterDTO getParameter(String parameterKey, String administratorId, String administratorRole) {
        ensureAuthorized(administratorId, administratorRole);
        return findOrThrow(parameterKey).toDTO();
    }

    @Override
    public ValidationResultDTO validateParameterUpdate(String parameterKey, String proposedValue,
                                                       String administratorId, String administratorRole) {
        ensureAuthorized(administratorId, administratorRole);
        ConfigurationParameter existing = findOrThrow(parameterKey);
        return existing.validateProposedValue(proposedValue);
    }

    @Override
    public UpdateParameterResponseDTO updateParameter(UpdateParameterRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");

        // A1. Unauthorized administrator
        ensureAuthorized(request.administratorId(), request.administratorRole());

        ConfigurationParameter existing = findOrThrow(request.parameterKey());

        // A4. Concurrent change (optimistic version check)
        if (existing.getVersion() != request.expectedVersion()) {
            throw new ConcurrentConfigurationChangeException(
                    existing.getKey(),
                    request.expectedVersion(),
                    existing.getVersion(),
                    existing.getCurrentValue()
            );
        }

        // A2. Invalid value
        ValidationResultDTO validation = existing.validateProposedValue(request.proposedValue());
        if (!validation.valid()) {
            throw new InvalidConfigurationValueException(existing.getKey(), validation.errorMessage());
        }

        String previousValue = existing.getCurrentValue();
        Instant now = Instant.now(clock);
        ConfigurationParameter updated = existing.withUpdatedValue(
                request.proposedValue(), request.administratorId(), now
        );

        // A3. Save failure (ensure previous value remains reported as active)
        ConfigurationParameter persisted;
        try {
            persisted = repository.save(updated);
        } catch (RuntimeException ex) {
            throw new ConfigurationSaveException(existing.getKey(), previousValue, ex);
        }

        ActivationStatus activationStatus = persisted.resolveActivationStatus();
        String message = buildActivationMessage(activationStatus);

        // <<include>> Record Audit Event (NFR-11)
        auditService.recordEvent(new AuditEventDTO(
                "CONFIGURATION_PARAMETER_UPDATED",
                request.administratorId(),
                persisted.getKey(),
                previousValue,
                persisted.getCurrentValue(),
                "ActivationStatus=" + activationStatus + "; version=" + persisted.getVersion(),
                now
        ));

        return new UpdateParameterResponseDTO(
                persisted.getKey(),
                previousValue,
                persisted.getCurrentValue(),
                persisted.getVersion(),
                activationStatus,
                message,
                request.administratorId(),
                now
        );
    }

    private void ensureAuthorized(String actorId, String role) {
        if (!accessControlService.canManageConfiguration(actorId, role)) {
            throw new UnauthorizedConfigurationAccessException(
                    actorId,
                    "Access denied: user '" + actorId + "' is not authorized to manage system configurations."
            );
        }
    }

    private ConfigurationParameter findOrThrow(String parameterKey) {
        if (parameterKey == null || parameterKey.isBlank()) {
            throw new ConfigurationNotFoundException(String.valueOf(parameterKey));
        }
        return repository.findByKey(parameterKey)
                .orElseThrow(() -> new ConfigurationNotFoundException(parameterKey));
    }

    private String buildActivationMessage(ActivationStatus status) {
        return switch (status) {
            case ACTIVE -> "Configuration saved and took effect immediately.";
            case PENDING_RELOAD -> "Configuration saved. A service reload is required for the change to take effect.";
            case PENDING_RESTART -> "Configuration saved. A system restart is required for the change to take effect.";
        };
    }
}
