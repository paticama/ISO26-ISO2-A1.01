package com.sescam.radiodiagnostics.integration.domain;

import com.sescam.radiodiagnostics.common.integration.IntegrationStatus;
import com.sescam.radiodiagnostics.common.integration.IntegrationSummaryDTO;
import com.sescam.radiodiagnostics.common.integration.IntegrationType;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity of C9: the saved configuration of one external system. Immutable: every change creates a new
 * instance, so a failed save never leaves a half-changed object. The credential is not part of this
 * entity; it lives in the secret store and only {@code credentialConfigured} is kept here.
 */
public final class Integration {

    private final String id;
    private final IntegrationType type;
    private final String displayName;
    private final String endpointUrl;
    private final String username;
    private final boolean credentialConfigured;
    private final IntegrationStatus status;
    private final String statusMessage;
    private final long version;
    private final Instant lastCheckedAt;
    private final String lastModifiedBy;
    private final Instant lastModifiedAt;

    public Integration(String id, IntegrationType type, String displayName, String endpointUrl, String username,
                       boolean credentialConfigured, IntegrationStatus status, String statusMessage, long version,
                       Instant lastCheckedAt, String lastModifiedBy, Instant lastModifiedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.endpointUrl = endpointUrl;
        this.username = username;
        this.credentialConfigured = credentialConfigured;
        this.status = Objects.requireNonNull(status, "status");
        this.statusMessage = statusMessage;
        this.version = version;
        this.lastCheckedAt = lastCheckedAt;
        this.lastModifiedBy = lastModifiedBy;
        this.lastModifiedAt = lastModifiedAt;
    }

    /** An integration that has not been configured yet. */
    public static Integration notConfigured(String id, IntegrationType type, String displayName, Instant now) {
        return new Integration(id, type, displayName, null, null, false, IntegrationStatus.NOT_CONFIGURED,
                "Not configured yet.", 1L, null, "system", now);
    }

    public boolean isConfigured() {
        return endpointUrl != null;
    }

    /** New saved settings: version goes up and the status is the result of the connection check. */
    public Integration withSettings(String newEndpointUrl, String newUsername, boolean hasCredential,
                                    IntegrationStatus newStatus, String newMessage, String modifiedBy, Instant now) {
        return new Integration(id, type, displayName, newEndpointUrl, newUsername, hasCredential, newStatus,
                newMessage, version + 1, now, modifiedBy, now);
    }

    /** New status after a connection check. The settings and the version do not change. */
    public Integration withStatus(IntegrationStatus newStatus, String newMessage, Instant checkedAt) {
        return new Integration(id, type, displayName, endpointUrl, username, credentialConfigured, newStatus,
                newMessage, version, checkedAt, lastModifiedBy, lastModifiedAt);
    }

    public IntegrationSummaryDTO toDTO() {
        return new IntegrationSummaryDTO(id, type, displayName, endpointUrl, username, credentialConfigured,
                status, statusMessage, version, lastCheckedAt, lastModifiedBy, lastModifiedAt);
    }

    public String getId() {
        return id;
    }

    public IntegrationType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public String getUsername() {
        return username;
    }

    public boolean isCredentialConfigured() {
        return credentialConfigured;
    }

    public IntegrationStatus getStatus() {
        return status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public long getVersion() {
        return version;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }
}
