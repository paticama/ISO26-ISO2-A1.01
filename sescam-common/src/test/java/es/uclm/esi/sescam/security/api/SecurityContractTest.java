package es.uclm.esi.sescam.security.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class SecurityContractTest {

    private final RoleDTO patient = new RoleDTO("Patient");

    @Test
    void roleRequiresName() {
        assertThatNullPointerException().isThrownBy(() -> new RoleDTO(null));
    }

    @Test
    void roleAssignmentCopiesRolesAndRejectsNulls() {
        List<RoleDTO> roles = new ArrayList<>(List.of(patient));
        RoleAssignmentDTO dto = new RoleAssignmentDTO("u1", "ACTIVE", roles);
        roles.clear();

        assertThat(dto.roles()).containsExactly(patient);
        assertThatThrownBy(() -> dto.roles().add(patient)).isInstanceOf(UnsupportedOperationException.class);
        assertThatNullPointerException().isThrownBy(() -> new RoleAssignmentDTO(null, "ACTIVE", List.of()));
        assertThatNullPointerException().isThrownBy(() -> new RoleAssignmentDTO("u1", null, List.of()));
        assertThatNullPointerException().isThrownBy(() -> new RoleAssignmentDTO("u1", "ACTIVE", null));
    }

    @Test
    void changeRequestRequiresAllFields() {
        RoleChangeRequestDTO dto = new RoleChangeRequestDTO("admin", "u1", "Physician");
        assertThat(dto.administratorId()).isEqualTo("admin");
        assertThat(dto.targetUserId()).isEqualTo("u1");
        assertThat(dto.roleName()).isEqualTo("Physician");
        assertThatNullPointerException().isThrownBy(() -> new RoleChangeRequestDTO(null, "u1", "r"));
        assertThatNullPointerException().isThrownBy(() -> new RoleChangeRequestDTO("a", null, "r"));
        assertThatNullPointerException().isThrownBy(() -> new RoleChangeRequestDTO("a", "u1", null));
    }

    @Test
    void changeResultCopiesListsAndRejectsNulls() {
        List<String> permissions = new ArrayList<>(List.of("P"));
        RoleChangeResultDTO dto = new RoleChangeResultDTO(ChangeOutcome.CONFIRMED, "ok", List.of(patient), permissions);
        permissions.clear();

        assertThat(dto.effectivePermissions()).containsExactly("P");
        assertThat(dto.outcome()).isEqualTo(ChangeOutcome.CONFIRMED);
        assertThatNullPointerException().isThrownBy(() -> new RoleChangeResultDTO(null, "m", List.of(), List.of()));
        assertThatNullPointerException().isThrownBy(
                () -> new RoleChangeResultDTO(ChangeOutcome.VALID, null, List.of(), List.of()));
        assertThatNullPointerException().isThrownBy(
                () -> new RoleChangeResultDTO(ChangeOutcome.VALID, "m", null, List.of()));
        assertThatNullPointerException().isThrownBy(
                () -> new RoleChangeResultDTO(ChangeOutcome.VALID, "m", List.of(), null));
    }

    @Test
    void auditEventAllowsNoTargetButRequiresTheRest() {
        Instant now = Instant.now();
        assertThat(new AuditEventDTO("a", "x", now, null, "ok").target()).isNull();
        assertThatNullPointerException().isThrownBy(() -> new AuditEventDTO(null, "x", now, null, "ok"));
        assertThatNullPointerException().isThrownBy(() -> new AuditEventDTO("a", null, now, null, "ok"));
        assertThatNullPointerException().isThrownBy(() -> new AuditEventDTO("a", "x", null, null, "ok"));
        assertThatNullPointerException().isThrownBy(() -> new AuditEventDTO("a", "x", now, null, null));
    }

    @Test
    void accessDeniedExceptionCarriesItsMessage() {
        assertThat(new AccessDeniedException("no")).hasMessage("no").isInstanceOf(RuntimeException.class);
    }

    @Test
    void outcomesCoverEveryFlowOfTheUseCase() {
        assertThat(ChangeOutcome.values()).containsExactlyInAnyOrder(ChangeOutcome.VALID, ChangeOutcome.CONFIRMED,
                ChangeOutcome.UNAUTHORIZED, ChangeOutcome.INVALID, ChangeOutcome.SAFEGUARD_REQUIRED,
                ChangeOutcome.SAVE_FAILED);
    }
}
