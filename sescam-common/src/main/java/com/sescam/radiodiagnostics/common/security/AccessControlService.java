package com.sescam.radiodiagnostics.common.security;

/**
 * Outbound port to C1 (sescam-security) for verifying role-based permissions (UC-ADM-02).
 */
public interface AccessControlService {

    /**
     * Checks whether the authenticated user has permission to manage system configurations.
     */
    boolean canManageConfiguration(String actorId, String role);

    /**
     * Checks whether the authenticated user may view and change the external integrations (UC-ADM-04).
     * Until C1 defines a separate permission, it is the same as managing the system configuration.
     */
    default boolean canManageIntegrations(String actorId, String role) {
        return canManageConfiguration(actorId, role);
    }
}
