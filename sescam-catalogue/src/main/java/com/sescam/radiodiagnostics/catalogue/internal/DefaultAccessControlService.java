package com.sescam.radiodiagnostics.catalogue.internal;

import com.sescam.radiodiagnostics.common.security.AccessControlService;

/**
 * Default RBAC implementation used when C1 (sescam-security) is not yet wired
 * (per PLAN18-11.md §3 & §7 no-op/fallback rule).
 */
public class DefaultAccessControlService implements AccessControlService {

    @Override
    public boolean canManageConfiguration(String actorId, String role) {
        if (actorId == null || actorId.isBlank() || role == null || role.isBlank()) {
            return false;
        }
        String normalized = role.trim().toUpperCase();
        return "ADMINISTRATOR".equals(normalized)
                || "SYSTEM_ADMINISTRATOR".equals(normalized)
                || "ADMIN".equals(normalized);
    }
}
