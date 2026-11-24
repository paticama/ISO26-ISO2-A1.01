package com.sescam.radiodiagnostics.common.catalogue;

public class ConfigurationNotFoundException extends RuntimeException {
    private final String parameterKey;

    public ConfigurationNotFoundException(String parameterKey) {
        super("Configuration parameter not found: " + parameterKey);
        this.parameterKey = parameterKey;
    }

    public String getParameterKey() {
        return parameterKey;
    }
}
