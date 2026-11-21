package es.uclm.esi.sescam.security.internal;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import es.uclm.esi.sescam.security.api.AccessControlService;
import es.uclm.esi.sescam.security.api.AccessDeniedException;
import es.uclm.esi.sescam.security.api.AuditEventDTO;
import es.uclm.esi.sescam.security.api.AuditService;
import es.uclm.esi.sescam.security.api.ChangeOutcome;
import es.uclm.esi.sescam.security.api.RoleAssignmentDTO;
import es.uclm.esi.sescam.security.api.RoleChangeRequestDTO;
import es.uclm.esi.sescam.security.api.RoleChangeResultDTO;
import es.uclm.esi.sescam.security.api.RoleDTO;

/**
 * Implementation of UC-ADM-02. A role change sets the user's role to the requested one.
 *
 * <p>Sensitive assignments (A4) are never saved: the applicable safeguards are not defined yet, so
 * the service fails closed and answers {@code SAFEGUARD_REQUIRED}.
 */
public class AccessControlServiceImpl implements AccessControlService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final AuditService audit;
    private final SensitiveAssignmentPolicy policy;
    private final Clock clock;

    public AccessControlServiceImpl(UserRepository users, RoleRepository roles, AuditService audit,
                                    SensitiveAssignmentPolicy policy) {
        this(users, roles, audit, policy, Clock.systemUTC());
    }

    public AccessControlServiceImpl(UserRepository users, RoleRepository roles, AuditService audit,
                                    SensitiveAssignmentPolicy policy, Clock clock) {
        this.users = Objects.requireNonNull(users);
        this.roles = Objects.requireNonNull(roles);
        this.audit = Objects.requireNonNull(audit);
        this.policy = Objects.requireNonNull(policy);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public RoleAssignmentDTO getRoleAssignments(String administratorId, String targetUserId) {
        if (!isAuthorized(administratorId)) {
            throw new AccessDeniedException("Not allowed to manage roles and permissions");
        }
        User target = users.findById(targetUserId)
                .orElseThrow(() -> new NoSuchElementException("Unknown user account: " + targetUserId));
        return new RoleAssignmentDTO(target.userId(), target.accountStatus(), toDtos(target.roles()));
    }

    @Override
    public RoleChangeResultDTO validateRoleChange(RoleChangeRequestDTO request) {
        Check check = check(request);
        if (check.rejection() != null) {
            return check.rejection();
        }
        return result(ChangeOutcome.VALID, "The change is valid", Set.of(check.role()));
    }

    @Override
    public RoleChangeResultDTO confirmRoleChange(RoleChangeRequestDTO request) {
        Check check = check(request);
        if (check.rejection() != null) {
            return check.rejection();
        }
        User previous = check.target();
        User updated = previous.withRoles(Set.of(check.role()));
        try {
            users.save(updated);
            audit.record(new AuditEventDTO(request.administratorId(), "ROLE_CHANGED", Instant.now(clock),
                    request.targetUserId(), "CONFIRMED: " + names(previous.roles()) + " -> " + names(updated.roles())));
        } catch (RuntimeException failure) {
            restore(previous);
            return result(ChangeOutcome.SAVE_FAILED, "The change could not be saved; access has not changed",
                    previous.roles());
        }
        return result(ChangeOutcome.CONFIRMED, "Role changed", updated.roles());
    }

    /** Validation shared by validate and confirm: returns the rejection, or the target and role. */
    private Check check(RoleChangeRequestDTO request) {
        if (!isAuthorized(request.administratorId())) {
            return reject(ChangeOutcome.UNAUTHORIZED, "Not allowed to manage roles and permissions", Set.of());
        }
        User target = users.findById(request.targetUserId()).orElse(null);
        if (target == null) {
            return reject(ChangeOutcome.INVALID, "The target user account does not exist", Set.of());
        }
        Role role = roles.findByName(request.roleName()).orElse(null);
        if (role == null) {
            return reject(ChangeOutcome.INVALID, "Unknown role: " + request.roleName(), target.roles());
        }
        if (target.roles().equals(Set.of(role))) {
            return reject(ChangeOutcome.INVALID, "The user already has the role " + role.name(), target.roles());
        }
        if (policy.isSensitive(request.administratorId(), request.targetUserId(), role)) {
            return reject(ChangeOutcome.SAFEGUARD_REQUIRED,
                    "This is a sensitive assignment and requires the applicable safeguards", target.roles());
        }
        return new Check(null, target, role);
    }

    private boolean isAuthorized(String administratorId) {
        return users.findById(administratorId)
                .map(admin -> admin.holds(Permissions.MANAGE_ROLES_AND_PERMISSIONS))
                .orElse(false);
    }

    /** Best effort: puts the previous state back after a failed save or audit. */
    private void restore(User previous) {
        try {
            users.save(previous);
        } catch (RuntimeException ignored) {
            // nothing more can be done here; the caller still reports SAVE_FAILED
        }
    }

    private Check reject(ChangeOutcome outcome, String message, Set<Role> currentRoles) {
        return new Check(result(outcome, message, currentRoles), null, null);
    }

    private RoleChangeResultDTO result(ChangeOutcome outcome, String message, Set<Role> shownRoles) {
        List<String> permissions = shownRoles.stream()
                .flatMap(role -> role.permissions().stream())
                .map(Permission::name)
                .distinct()
                .sorted()
                .toList();
        return new RoleChangeResultDTO(outcome, message, toDtos(shownRoles), permissions);
    }

    private static List<RoleDTO> toDtos(Set<Role> source) {
        return source.stream().map(Role::name).sorted(Comparator.naturalOrder()).map(RoleDTO::new).toList();
    }

    private static List<String> names(Set<Role> source) {
        return source.stream().map(Role::name).sorted().toList();
    }

    private record Check(RoleChangeResultDTO rejection, User target, Role role) {
    }
}
