package com.sescam.radiodiagnostics.common.orders;

import java.util.List;

/**
 * Outbound port for reading a patient's active safety constraints (contrast allergy, renal impairment, pregnancy,
 * ferromagnetic implant) from the EHR/HCE (ADR-003, ADR-007).
 *
 * <p>Defined here, implemented by C9 (sescam-integration). C4 provides a no-op implementation that returns no
 * constraints until the real connector exists.
 */
public interface PatientSafetyGateway {

    /**
     * Returns every active constraint registered for the patient; empty when there are none.
     * The compatibility with a given examination is decided by C4, not by the gateway.
     */
    List<SafetyConstraintDTO> getActiveConstraints(String patientId);
}
