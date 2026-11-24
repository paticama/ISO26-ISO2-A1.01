package com.sescam.radiodiagnostics.common.integration;

/**
 * A1: the user may not view or change integrations. The message never reveals protected settings.
 */
public class UnauthorizedIntegrationAccessException extends RuntimeException {
    private final String actorId;

    public UnauthorizedIntegrationAccessException(String actorId, String message) {
        super(message);
        this.actorId = actorId;
    }

    public String getActorId() {
        return actorId;
    }
}
