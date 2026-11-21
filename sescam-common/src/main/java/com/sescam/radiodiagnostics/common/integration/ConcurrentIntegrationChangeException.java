package com.sescam.radiodiagnostics.common.integration;

/**
 * Another administrator changed the integration after it was loaded (optimistic concurrency).
 */
public class ConcurrentIntegrationChangeException extends RuntimeException {
    private final String integrationId;
    private final long expectedVersion;
    private final long actualVersion;

    public ConcurrentIntegrationChangeException(String integrationId, long expectedVersion, long actualVersion) {
        super("Integration '" + integrationId + "' was changed by someone else (expected version "
                + expectedVersion + ", current version " + actualVersion + "). Reload and try again.");
        this.integrationId = integrationId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public String getIntegrationId() {
        return integrationId;
    }

    public long getExpectedVersion() {
        return expectedVersion;
    }

    public long getActualVersion() {
        return actualVersion;
    }
}
