package com.sescam.radiodiagnostics.client.catalogue;

import com.sescam.radiodiagnostics.common.catalogue.ConfigurationParameterDTO;
import com.sescam.radiodiagnostics.common.catalogue.UpdateParameterResponseDTO;

import java.util.List;

/**
 * View contract for the System Administrator configuration screen (UC-ADM-01, #120).
 */
public interface ConfigurationAdminView {

    void setSaveActionEnabled(boolean enabled);

    void displayParameters(List<ConfigurationParameterDTO> parameters);

    void displayValidationError(String parameterKey, String errorMessage);

    void displayUpdateSuccess(UpdateParameterResponseDTO response);

    void displayConflictWarning(String parameterKey, String currentStoredValue, long actualVersion, String message);

    void displayError(String message);
}
