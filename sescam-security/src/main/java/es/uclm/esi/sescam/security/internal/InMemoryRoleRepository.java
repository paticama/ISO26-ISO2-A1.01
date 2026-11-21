package es.uclm.esi.sescam.security.internal;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory {@link RoleRepository}, used until the persistence technology is decided. */
public class InMemoryRoleRepository implements RoleRepository {

    private final Map<String, Role> roles = new ConcurrentHashMap<>();

    public void add(Role role) {
        roles.put(role.name(), role);
    }

    @Override
    public Optional<Role> findByName(String name) {
        return Optional.ofNullable(roles.get(name));
    }
}
