package com.sescam.radiodiagnostics.common.catalogue;

public class InvalidConfigurationValueException extends RuntimeException {
    private final String parameterKey;

    public InvalidConfigurationValueException(String parameterKey, String reason) {
        super("Invalid value for parameter '" + parameterKey + "': " + reason);
        this.parameterKey = parameterKey;
    }

    public String getParameterKey() {
        return parameterKey;
    }
}
