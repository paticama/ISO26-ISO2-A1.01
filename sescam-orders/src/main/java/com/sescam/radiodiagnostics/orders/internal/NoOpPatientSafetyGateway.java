package com.sescam.radiodiagnostics.orders.internal;

import com.sescam.radiodiagnostics.common.orders.PatientSafetyGateway;
import com.sescam.radiodiagnostics.common.orders.SafetyConstraintDTO;

import java.util.List;

/**
 * Stand-in for the EHR connector of C9 (sescam-integration): reports no active constraints.
 * Replace it when the connector exists.
 */
public class NoOpPatientSafetyGateway implements PatientSafetyGateway {

    @Override
    public List<SafetyConstraintDTO> getActiveConstraints(String patientId) {
        return List.of();
    }
}
