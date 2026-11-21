package com.sescam.radiodiagnostics.server.security;

import com.sescam.radiodiagnostics.common.security.AuditQueryService;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.security.internal.AuditAccessPolicy;
import com.sescam.radiodiagnostics.security.repository.AuditEventRepository;
import com.sescam.radiodiagnostics.security.repository.InMemoryAuditEventRepository;
import com.sescam.radiodiagnostics.security.service.AuditQueryServiceImpl;
import com.sescam.radiodiagnostics.security.service.AuditServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;

/**
 * Wires the C1 audit services (UC-ADM-03). {@code securityAuditService} is {@link Primary} so that it
 * replaces the no-op {@code AuditService} beans other modules register while C1 was not built.
 */
@Configuration
public class SecurityAuditWiringConfig {

    @Bean
    public Clock auditClock() {
        return Clock.systemUTC();
    }

    @Bean
    public AuditEventRepository auditEventRepository() {
        return new InMemoryAuditEventRepository();
    }

    @Bean
    @Primary
    public AuditService securityAuditService(AuditEventRepository auditEventRepository, Clock auditClock) {
        return new AuditServiceImpl(auditEventRepository, auditClock);
    }

    @Bean
    public AuditQueryService auditQueryService(AuditEventRepository auditEventRepository,
                                               AuditService securityAuditService, Clock auditClock) {
        return new AuditQueryServiceImpl(auditEventRepository, securityAuditService, new AuditAccessPolicy(),
                auditClock);
    }
}
