package com.sescam.radiodiagnostics.common.integration;

import java.util.List;

/**
 * Result of checking proposed settings before they are confirmed (main flow, step 5). Nothing is saved.
 *
 * @param valid      false when the settings are not acceptable; {@code errors} then says why
 * @param errors     validation messages, never containing secrets
 * @param connection result of the connection check; null when the settings are not valid
 */
public record SettingsCheckResultDTO(
        String integrationId,
        boolean valid,
        List<String> errors,
        ConnectionCheckResultDTO connection
) {
    public static SettingsCheckResultDTO invalid(String integrationId, List<String> errors) {
        return new SettingsCheckResultDTO(integrationId, false, List.copyOf(errors), null);
    }

    public static SettingsCheckResultDTO checked(String integrationId, ConnectionCheckResultDTO connection) {
        return new SettingsCheckResultDTO(integrationId, true, List.of(), connection);
    }

    /** True when the settings are valid and the connection check did not fail, so saving makes sense. */
    public boolean canSave() {
        return valid && (connection == null || !connection.status().isFailure());
    }
}
