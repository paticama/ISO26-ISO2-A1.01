package es.uclm.esi.sescam.security.internal;

import java.util.Objects;
import java.util.Set;

/** A named set of permissions. A role grants at least one permission. */
public record Role(String name, Set<Permission> permissions) {

    public Role {
        Objects.requireNonNull(name, "name");
        permissions = Set.copyOf(Objects.requireNonNull(permissions, "permissions"));
        if (permissions.isEmpty()) {
            throw new IllegalArgumentException("A role must grant at least one permission");
        }
    }
}
