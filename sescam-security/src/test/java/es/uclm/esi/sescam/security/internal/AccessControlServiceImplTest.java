package es.uclm.esi.sescam.security.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import es.uclm.esi.sescam.security.api.AccessDeniedException;
import es.uclm.esi.sescam.security.api.AuditEventDTO;
import es.uclm.esi.sescam.security.api.AuditService;
import es.uclm.esi.sescam.security.api.ChangeOutcome;
import es.uclm.esi.sescam.security.api.RoleAssignmentDTO;
import es.uclm.esi.sescam.security.api.RoleChangeRequestDTO;
import es.uclm.esi.sescam.security.api.RoleChangeResultDTO;
import es.uclm.esi.sescam.security.api.RoleDTO;

class AccessControlServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    private final Role adminRole = new Role("System administrator", Set.of(Permissions.MANAGE_ROLES_AND_PERMISSIONS));
    private final Role patientRole = new Role("Patient", Set.of(new Permission("VIEW_OWN_APPOINTMENTS")));
    private final Role physicianRole = new Role("Physician",
            Set.of(new Permission("VIEW_OWN_APPOINTMENTS"), new Permission("ORDER_PROCEDURES")));

    private InMemoryUserRepository users;
    private InMemoryRoleRepository roles;
    private AuditService audit;
    private AccessControlServiceImpl service;

    @BeforeEach
    void setUp() {
        InMemoryUserRepository seeded = new InMemoryUserRepository();
        seeded.save(new User("admin", "ACTIVE", Set.of(adminRole)));
        seeded.save(new User("ana", "ACTIVE", Set.of(patientRole)));
        seeded.save(new User("pat", "ACTIVE", Set.of(patientRole)));
        users = spy(seeded);
        roles = new InMemoryRoleRepository();
        audit = mock(AuditService.class);
        roles.add(adminRole);
        roles.add(patientRole);
        roles.add(physicianRole);
        service = new AccessControlServiceImpl(users, roles, audit, new SensitiveAssignmentPolicy(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private RoleChangeRequestDTO request(String admin, String target, String role) {
        return new RoleChangeRequestDTO(admin, target, role);
    }

    private Set<Role> rolesOf(String userId) {
        return users.findById(userId).orElseThrow().roles();
    }

    // ---- criterion 1: an authorized administrator can view current role assignments

    @Test
    void authorizedAdministratorViewsCurrentAssignmentsAndStatus() {
        RoleAssignmentDTO dto = service.getRoleAssignments("admin", "ana");

        assertThat(dto.userId()).isEqualTo("ana");
        assertThat(dto.accountStatus()).isEqualTo("ACTIVE");
        assertThat(dto.roles()).containsExactly(new RoleDTO("Patient"));
    }

    @Test
    void viewingIsDeniedToRequestersWithoutThePermission() {
        assertThatThrownBy(() -> service.getRoleAssignments("ana", "pat")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.getRoleAssignments("ghost", "pat")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void viewingAnUnknownUserFails() {
        assertThatThrownBy(() -> service.getRoleAssignments("admin", "ghost"))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ---- criterion 2: roles can be assigned or changed according to the rules

    @Test
    void validChangeIsValidatedWithoutSavingAnything() {
        RoleChangeResultDTO result = service.validateRoleChange(request("admin", "ana", "Physician"));

        assertThat(result.outcome()).isEqualTo(ChangeOutcome.VALID);
        assertThat(result.roles()).containsExactly(new RoleDTO("Physician"));
        assertThat(result.effectivePermissions()).containsExactly("ORDER_PROCEDURES", "VIEW_OWN_APPOINTMENTS");
        assertThat(rolesOf("ana")).containsExactly(patientRole);
        verify(users, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void confirmedChangeIsSavedAndShowsTheEffectiveResult() {
        RoleChangeResultDTO result = service.confirmRoleChange(request("admin", "ana", "Physician"));

        assertThat(result.outcome()).isEqualTo(ChangeOutcome.CONFIRMED);
        assertThat(result.roles()).containsExactly(new RoleDTO("Physician"));
        assertThat(result.effectivePermissions()).containsExactly("ORDER_PROCEDURES", "VIEW_OWN_APPOINTMENTS");
        assertThat(rolesOf("ana")).containsExactly(physicianRole);
    }

    // ---- criterion 3: invalid, disallowed or unauthorized changes do not alter the assignment (A1, A2)

    @Test
    void unauthorizedRequesterIsRejectedAndNothingChanges() {
        for (String requester : new String[] {"ana", "ghost"}) {
            RoleChangeResultDTO validate = service.validateRoleChange(request(requester, "pat", "Physician"));
            RoleChangeResultDTO confirm = service.confirmRoleChange(request(requester, "pat", "Physician"));

            assertThat(validate.outcome()).isEqualTo(ChangeOutcome.UNAUTHORIZED);
            assertThat(confirm.outcome()).isEqualTo(ChangeOutcome.UNAUTHORIZED);
            assertThat(confirm.roles()).isEmpty();
        }
        assertThat(rolesOf("pat")).containsExactly(patientRole);
        verify(users, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void unknownRoleIsInvalidAndKeepsTheCurrentRoles() {
        RoleChangeResultDTO result = service.confirmRoleChange(request("admin", "ana", "Wizard"));

        assertThat(result.outcome()).isEqualTo(ChangeOutcome.INVALID);
        assertThat(result.message()).contains("Wizard");
        assertThat(result.roles()).containsExactly(new RoleDTO("Patient"));
        assertThat(rolesOf("ana")).containsExactly(patientRole);
        verifyNoInteractions(audit);
    }

    @Test
    void unknownTargetUserIsInvalid() {
        RoleChangeResultDTO result = service.validateRoleChange(request("admin", "ghost", "Physician"));

        assertThat(result.outcome()).isEqualTo(ChangeOutcome.INVALID);
        assertThat(result.roles()).isEmpty();
    }

    @Test
    void assigningTheRoleTheUserAlreadyHasIsInvalid() {
        RoleChangeResultDTO result = service.confirmRoleChange(request("admin", "ana", "Patient"));

        assertThat(result.outcome()).isEqualTo(ChangeOutcome.INVALID);
        verify(users, never()).save(any());
    }

    // ---- criterion 4: sensitive changes receive safeguards (A4)

    @Test
    void changingTheAdministratorsOwnPrivilegesRequiresSafeguardsAndIsNotSaved() {
        RoleChangeResultDTO validate = service.validateRoleChange(request("admin", "admin", "Patient"));
        RoleChangeResultDTO confirm = service.confirmRoleChange(request("admin", "admin", "Patient"));

        assertThat(validate.outcome()).isEqualTo(ChangeOutcome.SAFEGUARD_REQUIRED);
        assertThat(confirm.outcome()).isEqualTo(ChangeOutcome.SAFEGUARD_REQUIRED);
        assertThat(rolesOf("admin")).containsExactly(adminRole);
        verify(users, never()).save(any());
    }

    @Test
    void designatedSensitiveRolesRequireSafeguards() {
        AccessControlServiceImpl strict = new AccessControlServiceImpl(users, roles, audit,
                new SensitiveAssignmentPolicy(Set.of("Physician")));

        assertThat(strict.confirmRoleChange(request("admin", "ana", "Physician")).outcome())
                .isEqualTo(ChangeOutcome.SAFEGUARD_REQUIRED);
        assertThat(rolesOf("ana")).containsExactly(patientRole);
    }

    // ---- A3: save failure never claims that access changed

    @Test
    void saveFailureIsReportedAndTheAssignmentIsPreserved() {
        doThrow(new IllegalStateException("db down")).doNothing().when(users).save(any());

        RoleChangeResultDTO result = service.confirmRoleChange(request("admin", "ana", "Physician"));

        assertThat(result.outcome()).isEqualTo(ChangeOutcome.SAVE_FAILED);
        assertThat(result.roles()).containsExactly(new RoleDTO("Patient"));
        verifyNoInteractions(audit);
    }

    @Test
    void failureWhileRestoringStillReportsSaveFailed() {
        doThrow(new IllegalStateException("db down")).when(users).save(any());

        assertThat(service.confirmRoleChange(request("admin", "ana", "Physician")).outcome())
                .isEqualTo(ChangeOutcome.SAVE_FAILED);
    }

    @Test
    void auditFailureRollsTheChangeBack() {
        doThrow(new IllegalStateException("audit down")).when(audit).record(any());

        RoleChangeResultDTO result = service.confirmRoleChange(request("admin", "ana", "Physician"));

        assertThat(result.outcome()).isEqualTo(ChangeOutcome.SAVE_FAILED);
        assertThat(rolesOf("ana")).containsExactly(patientRole);
        verify(users, times(2)).save(any());
    }

    // ---- criterion 5: successful changes are confirmed, attributable and available for audit

    @Test
    void successfulChangeIsRecordedWithActorActionTimestampAndTarget() {
        doNothing().when(audit).record(any());

        service.confirmRoleChange(request("admin", "ana", "Physician"));

        ArgumentCaptor<AuditEventDTO> captor = ArgumentCaptor.forClass(AuditEventDTO.class);
        verify(audit).record(captor.capture());
        AuditEventDTO event = captor.getValue();
        assertThat(event.actor()).isEqualTo("admin");
        assertThat(event.action()).isEqualTo("ROLE_CHANGED");
        assertThat(event.timestamp()).isEqualTo(NOW);
        assertThat(event.target()).isEqualTo("ana");
        assertThat(event.outcome()).contains("Patient").contains("Physician");
    }
}
