package es.uclm.esi.sescam.security.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Set;

import org.junit.jupiter.api.Test;

class DomainTest {

    private final Permission manage = Permissions.MANAGE_ROLES_AND_PERMISSIONS;
    private final Permission read = new Permission("READ");
    private final Role admin = new Role("System administrator", Set.of(manage));
    private final Role patient = new Role("Patient", Set.of(read));

    @Test
    void roleNeedsAtLeastOnePermission() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Role("Empty", Set.of()));
        assertThatNullPointerException().isThrownBy(() -> new Role(null, Set.of(read)));
        assertThatNullPointerException().isThrownBy(() -> new Permission(null));
    }

    @Test
    void userNeedsAtLeastOneRole() {
        assertThatIllegalArgumentException().isThrownBy(() -> new User("u1", "ACTIVE", Set.of()));
        assertThatNullPointerException().isThrownBy(() -> new User(null, "ACTIVE", Set.of(patient)));
        assertThatNullPointerException().isThrownBy(() -> new User("u1", null, Set.of(patient)));
    }

    @Test
    void userHoldsPermissionsThroughItsRoles() {
        User user = new User("u1", "ACTIVE", Set.of(patient));
        assertThat(user.holds(read)).isTrue();
        assertThat(user.holds(manage)).isFalse();
        assertThat(user.withRoles(Set.of(admin)).holds(manage)).isTrue();
        assertThat(user.withRoles(Set.of(admin)).userId()).isEqualTo("u1");
    }

    @Test
    void policyFlagsSelfChangesAndDesignatedRoles() {
        SensitiveAssignmentPolicy none = new SensitiveAssignmentPolicy();
        assertThat(none.isSensitive("a", "a", patient)).isTrue();
        assertThat(none.isSensitive("a", "b", patient)).isFalse();

        SensitiveAssignmentPolicy designated = new SensitiveAssignmentPolicy(Set.of("System administrator"));
        assertThat(designated.isSensitive("a", "b", admin)).isTrue();
        assertThat(designated.isSensitive("a", "b", patient)).isFalse();
    }

    @Test
    void inMemoryRepositoriesStoreAndFind() {
        InMemoryUserRepository users = new InMemoryUserRepository();
        InMemoryRoleRepository roles = new InMemoryRoleRepository();
        User user = new User("u1", "ACTIVE", Set.of(patient));
        users.save(user);
        roles.add(patient);

        assertThat(users.findById("u1")).contains(user);
        assertThat(users.findById("nobody")).isEmpty();
        assertThat(roles.findByName("Patient")).contains(patient);
        assertThat(roles.findByName("Nope")).isEmpty();
    }

    @Test
    void noOpAuditAcceptsEvents() {
        new NoOpAuditService().record(null);
    }
}
