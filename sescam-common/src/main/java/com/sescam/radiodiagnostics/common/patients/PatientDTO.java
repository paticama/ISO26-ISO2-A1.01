package com.sescam.radiodiagnostics.common.patients;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Full patient profile representation across module boundaries.
 */
public record PatientDTO(
        String id,
        String cip,
        String dni,
        String firstName,
        String lastName,
        LocalDate birthDate,
        Gender gender,
        String phone,
        String email,
        AddressDTO address,
        PreferredNotificationChannel preferredChannel,
        EmergencyContactDTO emergencyContact,
        String clinicalSummaryNotes,
        PatientStatus status,
        long version,
        Instant createdAt,
        Instant updatedAt,
        String ehrSyncStatus,
        Instant ehrSyncTimestamp
) {
}
