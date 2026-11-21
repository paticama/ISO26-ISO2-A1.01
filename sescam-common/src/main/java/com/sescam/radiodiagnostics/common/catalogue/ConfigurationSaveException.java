package com.sescam.radiodiagnostics.common.catalogue;

public class ConfigurationSaveException extends RuntimeException {
    private final String parameterKey;
    private final String previousValue;

    public ConfigurationSaveException(String parameterKey, String previousValue, Throwable cause) {
        super("Failed to save parameter '" + parameterKey
                + "'. The previous value ('" + previousValue + "') remains active.", cause);
        this.parameterKey = parameterKey;
        this.previousValue = previousValue;
    }

    public String getParameterKey() {
        return parameterKey;
    }

    public String getPreviousValue() {
        return previousValue;
    }
}
