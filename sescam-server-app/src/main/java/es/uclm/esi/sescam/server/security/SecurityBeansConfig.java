package es.uclm.esi.sescam.server.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.sescam.radiodiagnostics.server.security.SecurityAuditWiringConfig;

import es.uclm.esi.sescam.security.api.AccessControlService;
import es.uclm.esi.sescam.security.api.AuditService;
import es.uclm.esi.sescam.security.internal.AccessControlServiceImpl;
import es.uclm.esi.sescam.security.internal.InMemoryRoleRepository;
import es.uclm.esi.sescam.security.internal.InMemoryUserRepository;
import es.uclm.esi.sescam.security.internal.RoleRepository;
import es.uclm.esi.sescam.security.internal.SensitiveAssignmentPolicy;
import es.uclm.esi.sescam.security.internal.UserRepository;

/**
 * Wires sescam-security. Persistence is in memory until the database decision exists.
 * Auditing uses the UC-ADM-03 audit trail ({@link SecurityAuditWiringConfig}), so every role change
 * can be searched in the audit and compliance area.
 */
@Configuration(proxyBeanMethods = false)
@Import(SecurityAuditWiringConfig.class)
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
    AccessControlService accessControlService(UserRepository users, RoleRepository roles, AuditService audit) {
        return new AccessControlServiceImpl(users, roles, audit, new SensitiveAssignmentPolicy());
    }
}
