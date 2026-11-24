package com.sescam.radiodiagnostics.schedules.internal;

import java.util.Locale;
import java.util.Set;

/**
 * Roles of UC-AAD-02 (A6). Admissions Staff operates schedules; the System Administrator and the clinical
 * secondary actors (physician, medical staff) can only consult them, so administrator configuration stays
 * distinct from Admissions Staff operations. Kept inside C3, like {@code AuditAccessPolicy} in C1, until the
 * RBAC permissions of UC-ADM-02 are merged.
 */
public class ScheduleAccessPolicy {

    private static final Set<String> OPERATOR_ROLES =
            Set.of("ADMISSIONS_STAFF", "ADMISSIONS", "ADMINISTRATIVE_STAFF", "RECEPTIONIST");

    private static final Set<String> VIEWER_ROLES = Set.of("SYSTEM_ADMINISTRATOR", "ADMINISTRATOR", "ADMIN",
            "PHYSICIAN", "RADIOLOGIST", "MEDICAL_STAFF", "TECHNICIAN", "RADIOLOGY_TECHNICIAN");

    /** Open, update, close and reopen. */
    public boolean canOperateSchedules(String actorId, String role) {
        return hasActor(actorId) && OPERATOR_ROLES.contains(normalize(role));
    }

    /** Search and consult. */
    public boolean canViewSchedules(String actorId, String role) {
        return canOperateSchedules(actorId, role) || (hasActor(actorId) && VIEWER_ROLES.contains(normalize(role)));
    }

    private static boolean hasActor(String actorId) {
        return actorId != null && !actorId.isBlank();
    }

    private static String normalize(String role) {
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }
}
