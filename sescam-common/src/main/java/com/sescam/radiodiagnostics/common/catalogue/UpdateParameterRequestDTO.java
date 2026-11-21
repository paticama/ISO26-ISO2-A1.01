package com.sescam.radiodiagnostics.common.catalogue;

/**
 * Request payload to update a configuration parameter with optimistic concurrency control.
 */
public record UpdateParameterRequestDTO(
        String parameterKey,
        String proposedValue,
        long expectedVersion,
        String administratorId,
        String administratorRole
) {
}
