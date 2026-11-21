package com.sescam.radiodiagnostics.common.catalogue;

public class UnauthorizedConfigurationAccessException extends RuntimeException {
    private final String actorId;

    public UnauthorizedConfigurationAccessException(String actorId, String message) {
        super(message);
        this.actorId = actorId;
    }

    public String getActorId() {
        return actorId;
    }
}
