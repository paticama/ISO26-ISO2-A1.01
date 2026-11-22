package com.sescam.radiodiagnostics.integration.connection;

import com.sescam.radiodiagnostics.common.integration.IntegrationType;

/**
 * Probe used while the real connectors are not built (PLAN18-11 §3). It does not touch the network and
 * does not claim success: the status stays "not checked" instead of "connected".
 */
public class NoOpConnectionProbe implements ConnectionProbe {

    @Override
    public ProbeResult probe(IntegrationType type, String endpointUrl, String username, String secret) {
        return ProbeResult.notSupported("Connection check is not available yet for " + type
                + ". The settings were validated but the connection was not tested.");
    }
}
