package com.sescam.radiodiagnostics.security.internal;

import java.util.Locale;
import java.util.Set;

/**
 * Roles allowed to read audit records (UC-ADM-03 actor: system administrator or authorized
 * compliance reviewer). To be replaced by the RBAC permissions of UC-ADM-02 once they are merged.
 */
public class AuditAccessPolicy {

    private static final Set<String> AUDIT_ROLES =
            Set.of("ADMINISTRATOR", "SYSTEM_ADMINISTRATOR", "ADMIN", "COMPLIANCE_REVIEWER");

    public boolean canViewAuditRecords(String actorId, String role) {
        if (actorId == null || actorId.isBlank() || role == null || role.isBlank()) {
            return false;
        }
        return AUDIT_ROLES.contains(role.trim().toUpperCase(Locale.ROOT));
    }
}
