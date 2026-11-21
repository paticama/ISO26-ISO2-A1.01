package com.sescam.radiodiagnostics.client.catalogue;

import com.sescam.radiodiagnostics.common.catalogue.ConcurrentConfigurationChangeException;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationNotFoundException;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationParameterDTO;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationSaveException;
import com.sescam.radiodiagnostics.common.catalogue.InvalidConfigurationValueException;
import com.sescam.radiodiagnostics.common.catalogue.UnauthorizedConfigurationAccessException;
import com.sescam.radiodiagnostics.common.catalogue.UpdateParameterRequestDTO;
import com.sescam.radiodiagnostics.common.catalogue.UpdateParameterResponseDTO;
import com.sescam.radiodiagnostics.common.catalogue.ValidationResultDTO;

import java.util.List;
import java.util.Objects;

/**
 * Client-side MVC controller for UC-ADM-01 (System Configuration & Parameters view).
 * Enforces local field validation before REST submission and handles A1-A4 feedback.
 */
public class ConfigurationAdminController {

    private final ConfigurationRestClient restClient;
    private final ConfigurationFormValidator validator;
    private final ConfigurationAdminView view;

    public ConfigurationAdminController(ConfigurationRestClient restClient,
                                        ConfigurationFormValidator validator,
                                        ConfigurationAdminView view) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
        this.view = Objects.requireNonNull(view, "view must not be null");
    }

    /**
     * Opens the configuration area: hides save action for non-admin roles (Alt A1)
     * and loads available parameters from the server.
     */
    public void openConfigurationArea(String actorId, String actorRole) {
        boolean isAdmin = isAdminRole(actorRole);
        view.setSaveActionEnabled(isAdmin);

        if (!isAdmin) {
            view.displayError("Access denied: administrator permissions are required to manage system configuration.");
            return;
        }

        try {
            List<ConfigurationParameterDTO> parameters = restClient.fetchAllParameters(actorId, actorRole);
            view.displayParameters(parameters);
        } catch (UnauthorizedConfigurationAccessException ex) {
            view.setSaveActionEnabled(false);
            view.displayError(ex.getMessage());
        } catch (RuntimeException ex) {
            view.displayError("Unable to load configuration parameters: " + ex.getMessage());
        }
    }

    /**
     * Validates a proposed value locally before submitting to the server.
     */
    public boolean previewValidation(ConfigurationParameterDTO parameter, String proposedValue) {
        ValidationResultDTO result = validator.validate(parameter, proposedValue);
        if (!result.valid()) {
            view.displayValidationError(result.parameterKey(), result.errorMessage());
            return false;
        }
        return true;
    }

    /**
     * Validates locally, confirms, and submits the update to the server, reporting
     * activation status or alternative-flow errors (A1-A4).
     */
    public void confirmAndSave(ConfigurationParameterDTO parameter,
                               String proposedValue,
                               String actorId,
                               String actorRole) {
        if (!isAdminRole(actorRole)) {
            view.setSaveActionEnabled(false);
            view.displayError("Access denied: unauthorized save action blocked.");
            return;
        }

        if (!previewValidation(parameter, proposedValue)) {
            return;
        }

        UpdateParameterRequestDTO request = new UpdateParameterRequestDTO(
                parameter.key(),
                proposedValue.trim(),
                parameter.version(),
                actorId,
                actorRole
        );

        try {
            UpdateParameterResponseDTO response = restClient.updateParameter(request);
            view.displayUpdateSuccess(response);
        } catch (UnauthorizedConfigurationAccessException ex) {
            view.setSaveActionEnabled(false);
            view.displayError(ex.getMessage());
        } catch (InvalidConfigurationValueException ex) {
            view.displayValidationError(ex.getParameterKey(), ex.getMessage());
        } catch (ConcurrentConfigurationChangeException ex) {
            view.displayConflictWarning(
                    ex.getParameterKey(),
                    ex.getCurrentStoredValue(),
                    ex.getActualVersion(),
                    ex.getMessage()
            );
        } catch (ConfigurationSaveException ex) {
            view.displayError("Save failed. Previous value ('" + parameter.currentValue()
                    + "') remains active. Details: " + ex.getMessage());
        } catch (ConfigurationNotFoundException ex) {
            view.displayError(ex.getMessage());
        }
    }

    private boolean isAdminRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String normalized = role.trim().toUpperCase();
        return "ADMINISTRATOR".equals(normalized)
                || "SYSTEM_ADMINISTRATOR".equals(normalized)
                || "ADMIN".equals(normalized);
    }
}
