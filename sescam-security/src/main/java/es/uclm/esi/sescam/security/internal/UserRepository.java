package es.uclm.esi.sescam.security.internal;

import java.util.Optional;

/** Persistence port for user accounts. */
public interface UserRepository {

    Optional<User> findById(String userId);

    void save(User user);
}
