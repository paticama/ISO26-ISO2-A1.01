package com.sescam.radiodiagnostics.common.catalogue;

import java.util.List;

/**
 * Public contract for C2 (sescam-catalogue) supporting UC-ADM-01:
 * reviewing, validating, and updating system-wide configuration parameters.
 */
public interface CatalogueService {

    /**
     * Lists all system configuration parameters and their current values.
     */
    List<ConfigurationParameterDTO> getAllParameters(String administratorId, String administratorRole);

    /**
     * Retrieves a single configuration parameter by its unique key.
     */
    ConfigurationParameterDTO getParameter(String parameterKey, String administratorId, String administratorRole);

    /**
     * Validates a proposed value without persisting any change.
     */
    ValidationResultDTO validateParameterUpdate(String parameterKey, String proposedValue,
                                                String administratorId, String administratorRole);

    /**
     * Validates, persists, audits, and reports the activation status of a configuration update.
     */
    UpdateParameterResponseDTO updateParameter(UpdateParameterRequestDTO request);
}
