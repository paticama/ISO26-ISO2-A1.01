package es.uclm.esi.sescam.security.internal;

import java.util.Set;

/**
 * Decides which role assignments are sensitive (flow A4): changes to the administrator's own
 * privileges, and assignments of designated sensitive roles.
 */
public class SensitiveAssignmentPolicy {

    private final Set<String> designatedRoleNames;

    public SensitiveAssignmentPolicy(Set<String> designatedRoleNames) {
        this.designatedRoleNames = Set.copyOf(designatedRoleNames);
    }

    /** Policy with no designated sensitive roles; only self-changes are sensitive. */
    public SensitiveAssignmentPolicy() {
        this(Set.of());
    }

    public boolean isSensitive(String administratorId, String targetUserId, Role requestedRole) {
        return administratorId.equals(targetUserId) || designatedRoleNames.contains(requestedRole.name());
    }
}
