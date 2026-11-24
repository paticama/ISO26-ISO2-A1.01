package com.sescam.radiodiagnostics.integration.connection;

import com.sescam.radiodiagnostics.common.integration.IntegrationStatus;
import com.sescam.radiodiagnostics.common.integration.IntegrationType;

/**
 * Checks whether an external system accepts the given settings. The real connectors (SSO, HL7/FHIR, DICOM,
 * messaging) are outside UC-ADM-04, so each one will provide its own probe later. Implementations must not
 * put the secret or the original exception text into the messages they return.
 */
public interface ConnectionProbe {

    ProbeResult probe(IntegrationType type, String endpointUrl, String username, String secret);

    /**
     * Outcome of one probe.
     *
     * @param status  CONNECTED, CONNECTION_FAILED (refused), UNAVAILABLE (unreachable) or NOT_CHECKED (not supported)
     * @param message explanation for the administrator, without secrets
     */
    record ProbeResult(IntegrationStatus status, String message) {

        public static ProbeResult connected(String message) {
            return new ProbeResult(IntegrationStatus.CONNECTED, message);
        }

        public static ProbeResult refused(String message) {
            return new ProbeResult(IntegrationStatus.CONNECTION_FAILED, message);
        }

        public static ProbeResult unreachable(String message) {
            return new ProbeResult(IntegrationStatus.UNAVAILABLE, message);
        }

        public static ProbeResult notSupported(String message) {
            return new ProbeResult(IntegrationStatus.NOT_CHECKED, message);
        }
    }
}
