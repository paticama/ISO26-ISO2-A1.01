package com.sescam.radiodiagnostics.server.schedules;

import com.sescam.radiodiagnostics.common.catalogue.PolicyService;
import com.sescam.radiodiagnostics.common.schedules.ScheduleEventPublisher;
import com.sescam.radiodiagnostics.common.schedules.ScheduleService;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.schedules.internal.NoOpScheduleEventPublisher;
import com.sescam.radiodiagnostics.schedules.internal.ScheduleAccessPolicy;
import com.sescam.radiodiagnostics.schedules.internal.ScheduleRules;
import com.sescam.radiodiagnostics.schedules.repository.InMemoryScheduleRepository;
import com.sescam.radiodiagnostics.schedules.repository.ScheduleRepository;
import com.sescam.radiodiagnostics.schedules.service.ScheduleServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Wires module C3 (sescam-schedules, UC-AAD-02) into the server application. The schedule times are local
 * hospital times, so the service clock uses the hospital time zone ({@code sescam.schedules.zone}).
 * The event publisher is a no-op until C5 (sescam-admissions) provides the real {@link ScheduleEventPublisher};
 * UC-AAD-05 then replaces the {@code scheduleEventPublisher} bean.
 */
@Configuration
public class ScheduleWiringConfig {

    @Bean
    public ScheduleRepository scheduleRepository() {
        return new InMemoryScheduleRepository();
    }

    @Bean
    public ScheduleEventPublisher scheduleEventPublisher() {
        return new NoOpScheduleEventPublisher();
    }

    @Bean
    public ScheduleService scheduleService(ScheduleRepository scheduleRepository,
                                           PolicyService policyService,
                                           AuditService auditService,
                                           ScheduleEventPublisher scheduleEventPublisher,
                                           @Value("${sescam.schedules.zone:Europe/Madrid}") String zone) {
        return new ScheduleServiceImpl(scheduleRepository, new ScheduleRules(policyService),
                new ScheduleAccessPolicy(), auditService, scheduleEventPublisher, Clock.system(ZoneId.of(zone)));
    }
}
