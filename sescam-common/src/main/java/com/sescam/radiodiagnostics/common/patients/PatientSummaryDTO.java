package com.sescam.radiodiagnostics.common.patients;

import java.time.LocalDate;

/**
 * Compact projection for patient search listings and worklists.
 */
public record PatientSummaryDTO(
        String id,
        String cip,
        String dni,
        String fullName,
        LocalDate birthDate,
        String phone,
        PatientStatus status,
        long version
) {
}
