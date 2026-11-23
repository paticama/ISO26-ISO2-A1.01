package com.sescam.radiodiagnostics.patients.ehr;

import com.sescam.radiodiagnostics.common.patients.EhrGateway;
import com.sescam.radiodiagnostics.common.patients.EhrSyncResultDTO;
import com.sescam.radiodiagnostics.common.patients.PatientDTO;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Adapter simulating EHR synchronization until live C9 connectors are integrated.
 */
public class NoOpEhrGateway implements EhrGateway {

    private final AtomicBoolean simulateFailure = new AtomicBoolean(false);

    public void setSimulateFailure(boolean fail) {
        this.simulateFailure.set(fail);
    }

    @Override
    public EhrSyncResultDTO syncPatientRecord(PatientDTO patient) {
        if (simulateFailure.get()) {
            return EhrSyncResultDTO.failure("EHR host unavailable or connection refused");
        }
        return EhrSyncResultDTO.success("EHR-SESCAM-" + patient.cip());
    }
}
