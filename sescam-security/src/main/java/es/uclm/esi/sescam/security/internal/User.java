package es.uclm.esi.sescam.security.internal;

import java.util.Objects;
import java.util.Set;

/** A user account with its status and assigned roles. A user has at least one role. */
public record User(String userId, String accountStatus, Set<Role> roles) {

    public User {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(accountStatus, "accountStatus");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("A user must have at least one role");
        }
    }

    /** Returns a copy of this user with the given roles. */
    public User withRoles(Set<Role> newRoles) {
        return new User(userId, accountStatus, newRoles);
    }

    /** True if any of the user's roles grants the permission. */
    public boolean holds(Permission permission) {
        return roles.stream().anyMatch(role -> role.permissions().contains(permission));
    }
}
