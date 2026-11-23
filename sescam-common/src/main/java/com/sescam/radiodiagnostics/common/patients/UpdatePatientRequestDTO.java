package com.sescam.radiodiagnostics.common.patients;

public record UpdatePatientRequestDTO(
        String patientId,
        String firstName,
        String lastName,
        String phone,
        String email,
        AddressDTO address,
        PreferredNotificationChannel preferredChannel,
        EmergencyContactDTO emergencyContact,
        String clinicalSummaryNotes,
        long expectedVersion,
        String actorId,
        String actorRole
) {
}
