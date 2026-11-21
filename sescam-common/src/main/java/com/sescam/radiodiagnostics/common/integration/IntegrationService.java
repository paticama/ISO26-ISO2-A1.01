package com.sescam.radiodiagnostics.common.integration;

import java.util.List;

/**
 * Public contract for C9 (sescam-integration) supporting UC-ADM-04: configure and monitor the
 * integrations with external hospital systems.
 * <p>
 * Every operation checks that the administrator may manage integrations (A1). Credentials are write-only
 * and never appear in any result or exception (A4).
 */
public interface IntegrationService {

    /** Main flow, steps 1-2: lists the integrations with their current status. */
    List<IntegrationSummaryDTO> listIntegrations(String administratorId, String administratorRole);

    /** Main flow, step 3: one integration to configure or review. */
    IntegrationSummaryDTO getIntegration(String integrationId, String administratorId, String administratorRole);

    /**
     * Main flow, step 5: validates proposed settings and, where supported, checks the connection with them.
     * Nothing is saved. Invalid settings are reported in the result; a failed connection check is reported
     * in {@link SettingsCheckResultDTO#connection()}.
     */
    SettingsCheckResultDTO validateSettings(String integrationId, IntegrationSettingsDTO settings,
                                            String administratorId, String administratorRole);

    /**
     * Main flow, steps 6-7: validates again, checks the connection, saves, audits and reports the status.
     * If the validation or the connection check fails, an exception is thrown and the last working
     * configuration is kept (A2).
     */
    UpdateIntegrationResponseDTO updateIntegration(UpdateIntegrationRequestDTO request);

    /**
     * Checks the connection of the saved configuration again (retry or diagnostic review, A3) and stores
     * the new status. The settings are never changed by this operation.
     */
    ConnectionCheckResultDTO checkConnection(String integrationId, String administratorId, String administratorRole);
}
