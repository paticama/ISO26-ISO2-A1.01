package com.sescam.radiodiagnostics.common.catalogue;

public class ConcurrentConfigurationChangeException extends RuntimeException {
    private final String parameterKey;
    private final long expectedVersion;
    private final long actualVersion;
    private final String currentStoredValue;

    public ConcurrentConfigurationChangeException(String parameterKey, long expectedVersion,
                                                  long actualVersion, String currentStoredValue) {
        super("Concurrent modification detected for parameter '" + parameterKey
                + "': expected version " + expectedVersion + " but found version " + actualVersion
                + ". Reload and review the current value before saving.");
        this.parameterKey = parameterKey;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
        this.currentStoredValue = currentStoredValue;
    }

    public String getParameterKey() {
        return parameterKey;
    }

    public long getExpectedVersion() {
        return expectedVersion;
    }

    public long getActualVersion() {
        return actualVersion;
    }

    public String getCurrentStoredValue() {
        return currentStoredValue;
    }
}
