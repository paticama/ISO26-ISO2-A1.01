package es.uclm.esi.sescam.server.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.sescam.radiodiagnostics.common.security.AuditQueryService;
import com.sescam.radiodiagnostics.common.security.AuditRecordDTO;
import com.sescam.radiodiagnostics.common.security.AuditSearchCriteriaDTO;

import es.uclm.esi.sescam.security.api.AccessControlService;
import es.uclm.esi.sescam.security.api.ChangeOutcome;
import es.uclm.esi.sescam.security.api.RoleChangeRequestDTO;
import es.uclm.esi.sescam.security.internal.InMemoryRoleRepository;
import es.uclm.esi.sescam.security.internal.Permission;
import es.uclm.esi.sescam.security.internal.Permissions;
import es.uclm.esi.sescam.security.internal.Role;
import es.uclm.esi.sescam.security.internal.RoleRepository;
import es.uclm.esi.sescam.security.internal.User;
import es.uclm.esi.sescam.security.internal.UserRepository;

/**
 * UC-ADM-02 + UC-ADM-03: a confirmed role change is recorded in the audit trail and can be found
 * through the audit and compliance search ("successful role changes are available for audit").
 */
@SpringBootTest
class RoleChangeAuditIntegrationTest {

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private AuditQueryService auditQueryService;

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    @Test
    void confirmedRoleChangeIsSearchableInTheAuditTrail() {
        Role admin = new Role("System administrator", Set.of(Permissions.MANAGE_ROLES_AND_PERMISSIONS));
        Role patient = new Role("Patient", Set.of(new Permission("VIEW_OWN_APPOINTMENTS")));
        Role physician = new Role("Physician", Set.of(new Permission("ORDER_PROCEDURES")));
        ((InMemoryRoleRepository) roles).add(physician);
        users.save(new User("admin", "ACTIVE", Set.of(admin)));
        users.save(new User("ana", "ACTIVE", Set.of(patient)));
        Instant before = Instant.now();

        assertThat(accessControlService.confirmRoleChange(new RoleChangeRequestDTO("admin", "ana", "Physician"))
                .outcome()).isEqualTo(ChangeOutcome.CONFIRMED);

        AuditSearchCriteriaDTO criteria = new AuditSearchCriteriaDTO(before.minusSeconds(1),
                Instant.now().plusSeconds(1), "admin", "ROLE_CHANGED", "ana");
        assertThat(auditQueryService.searchEvents(criteria, "auditor-1", "COMPLIANCE_REVIEWER").events())
                .singleElement()
                .extracting(AuditRecordDTO::details).asString().contains("Physician");
    }
}
