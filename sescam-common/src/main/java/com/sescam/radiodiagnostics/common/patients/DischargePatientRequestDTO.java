package com.sescam.radiodiagnostics.common.patients;

public record DischargePatientRequestDTO(
        String patientId,
        String dischargeReason,
        long expectedVersion,
        String actorId,
        String actorRole
) {
}
