package com.sescam.radiodiagnostics.common.security;

/**
 * Outbound port to C1 (sescam-security) for verifying role-based permissions (UC-ADM-02).
 */
public interface AccessControlService {

    /**
     * Checks whether the authenticated user has permission to manage system configurations.
     */
    boolean canManageConfiguration(String actorId, String role);
}
