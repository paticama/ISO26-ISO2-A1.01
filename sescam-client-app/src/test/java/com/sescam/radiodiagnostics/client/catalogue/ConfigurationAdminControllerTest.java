package com.sescam.radiodiagnostics.client.catalogue;

import com.sescam.radiodiagnostics.common.catalogue.ActivationMode;
import com.sescam.radiodiagnostics.common.catalogue.ActivationStatus;
import com.sescam.radiodiagnostics.common.catalogue.ConcurrentConfigurationChangeException;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationNotFoundException;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationParameterDTO;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationSaveException;
import com.sescam.radiodiagnostics.common.catalogue.InvalidConfigurationValueException;
import com.sescam.radiodiagnostics.common.catalogue.ParameterValueType;
import com.sescam.radiodiagnostics.common.catalogue.UnauthorizedConfigurationAccessException;
import com.sescam.radiodiagnostics.common.catalogue.UpdateParameterResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConfigurationAdminControllerTest {

    private ConfigurationRestClient restClient;
    private ConfigurationAdminView view;
    private ConfigurationAdminController controller;
    private ConfigurationParameterDTO sampleParam;

    @BeforeEach
    void setUp() {
        restClient = mock(ConfigurationRestClient.class);
        view = mock(ConfigurationAdminView.class);
        controller = new ConfigurationAdminController(restClient, new ConfigurationFormValidator(), view);
        sampleParam = new ConfigurationParameterDTO(
                "booking.cancellation.min.hours", "Cancellation Window", "POLICIES", "desc",
                "24", ParameterValueType.INTEGER, 1.0, 168.0, List.of(),
                ActivationMode.IMMEDIATE, 1L, "system", Instant.now()
        );
    }

    @Test
    @DisplayName("Authorized administrator loads parameters and has save action enabled")
    void authorizedAdminOpensConfigurationArea() {
        when(restClient.fetchAllParameters("admin-1", "ADMINISTRATOR")).thenReturn(List.of(sampleParam));

        controller.openConfigurationArea("admin-1", "ADMINISTRATOR");

        verify(view).setSaveActionEnabled(true);
        verify(view).displayParameters(List.of(sampleParam));
    }

    @Test
    @DisplayName("Alt A1: Unauthorized role disables save action and does not call server")
    void unauthorizedRoleBlockedLocally() {
        controller.openConfigurationArea("pat-1", "PATIENT");

        verify(view).setSaveActionEnabled(false);
        verify(view).displayError(contains("Access denied"));
        verifyNoInteractions(restClient);

        controller.confirmAndSave(sampleParam, "48", "pat-1", "PATIENT");
        verifyNoInteractions(restClient);
    }

    @Test
    @DisplayName("Alt A2: Invalid local field value displays error and blocks server REST call")
    void invalidLocalValueBlocksRestCall() {
        assertFalse(controller.previewValidation(sampleParam, "999"));
        verify(view).displayValidationError(eq("booking.cancellation.min.hours"), contains("168"));

        controller.confirmAndSave(sampleParam, "not-an-int", "admin-1", "ADMINISTRATOR");
        verifyNoInteractions(restClient);
    }

    @Test
    @DisplayName("Main flow: Valid confirmed update calls REST client and displays activation status")
    void validUpdateCallsServerAndShowsConfirmation() {
        UpdateParameterResponseDTO resp = new UpdateParameterResponseDTO(
                "booking.cancellation.min.hours", "24", "48", 2L,
                ActivationStatus.ACTIVE, "Configuration saved and took effect immediately.",
                "admin-1", Instant.now()
        );
        when(restClient.updateParameter(any())).thenReturn(resp);

        controller.confirmAndSave(sampleParam, "48", "admin-1", "ADMINISTRATOR");

        verify(restClient).updateParameter(any());
        verify(view).displayUpdateSuccess(resp);
    }

    @Test
    @DisplayName("Alt A3 & A4 & Server errors: Controller maps server exceptions to clear view feedback")
    void serverErrorFlowsHandledCleanly() {
        // Alt A4: Concurrent modification
        doThrow(new ConcurrentConfigurationChangeException("booking.cancellation.min.hours", 1L, 2L, "36"))
                .when(restClient).updateParameter(any());
        controller.confirmAndSave(sampleParam, "48", "admin-1", "ADMINISTRATOR");
        verify(view).displayConflictWarning(eq("booking.cancellation.min.hours"), eq("36"), eq(2L), anyString());

        // Alt A3: Save failure -> makes clear previous value remains active
        doThrow(new ConfigurationSaveException("booking.cancellation.min.hours", "24", new RuntimeException("DB")))
                .when(restClient).updateParameter(any());
        controller.confirmAndSave(sampleParam, "48", "admin-1", "ADMINISTRATOR");
        verify(view).displayError(contains("Previous value ('24') remains active"));

        // Server-side 403
        doThrow(new UnauthorizedConfigurationAccessException("admin-1", "Revoked"))
                .when(restClient).updateParameter(any());
        controller.confirmAndSave(sampleParam, "48", "admin-1", "ADMINISTRATOR");
        verify(view).setSaveActionEnabled(false);

        // Server-side 400 and 404
        doThrow(new InvalidConfigurationValueException("booking.cancellation.min.hours", "Rejected"))
                .when(restClient).updateParameter(any());
        controller.confirmAndSave(sampleParam, "48", "admin-1", "ADMINISTRATOR");
        verify(view).displayValidationError(eq("booking.cancellation.min.hours"), contains("Rejected"));

        doThrow(new ConfigurationNotFoundException("booking.cancellation.min.hours"))
                .when(restClient).updateParameter(any());
        controller.confirmAndSave(sampleParam, "48", "admin-1", "ADMINISTRATOR");
        verify(view, atLeastOnce()).displayError(contains("not found"));
    }
}
