package es.uclm.esi.sescam.server.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import es.uclm.esi.sescam.security.api.AccessControlService;
import es.uclm.esi.sescam.security.api.AuditService;
import es.uclm.esi.sescam.security.internal.AccessControlServiceImpl;
import es.uclm.esi.sescam.security.internal.InMemoryRoleRepository;
import es.uclm.esi.sescam.security.internal.InMemoryUserRepository;
import es.uclm.esi.sescam.security.internal.NoOpAuditService;
import es.uclm.esi.sescam.security.internal.RoleRepository;
import es.uclm.esi.sescam.security.internal.SensitiveAssignmentPolicy;
import es.uclm.esi.sescam.security.internal.UserRepository;

/**
 * Wires sescam-security. Persistence is in memory and auditing is a no-op until the database
 * decision and UC-ADM-03 exist; replace these beans then.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityBeansConfig {

    @Bean
    UserRepository userRepository() {
        return new InMemoryUserRepository();
    }

    @Bean
    RoleRepository roleRepository() {
        return new InMemoryRoleRepository();
    }

    @Bean
    AuditService auditService() {
        return new NoOpAuditService();
    }

    @Bean
    AccessControlService accessControlService(UserRepository users, RoleRepository roles, AuditService audit) {
        return new AccessControlServiceImpl(users, roles, audit, new SensitiveAssignmentPolicy());
    }
}
