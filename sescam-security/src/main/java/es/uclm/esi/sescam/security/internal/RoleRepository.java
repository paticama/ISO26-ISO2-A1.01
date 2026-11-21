package es.uclm.esi.sescam.security.internal;

import java.util.Optional;

/** Persistence port for roles. */
public interface RoleRepository {

    Optional<Role> findByName(String name);
}
