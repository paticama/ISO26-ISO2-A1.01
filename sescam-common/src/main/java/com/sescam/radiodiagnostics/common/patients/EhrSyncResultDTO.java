package com.sescam.radiodiagnostics.common.patients;

import java.time.Instant;

public record EhrSyncResultDTO(
        boolean synced,
        String externalReference,
        String message,
        Instant syncTimestamp
) {
    public static EhrSyncResultDTO success(String ref) {
        return new EhrSyncResultDTO(true, ref, "Synchronized with EHR", Instant.now());
    }

    public static EhrSyncResultDTO failure(String message) {
        return new EhrSyncResultDTO(false, null, message, Instant.now());
    }
}
