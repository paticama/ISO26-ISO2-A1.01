package com.sescam.radiodiagnostics.common.patients;

import java.time.LocalDate;

public record CreatePatientRequestDTO(
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
        String actorId,
        String actorRole
) {
}
