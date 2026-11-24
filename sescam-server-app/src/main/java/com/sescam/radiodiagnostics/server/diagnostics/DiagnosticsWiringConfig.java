package com.sescam.radiodiagnostics.server.diagnostics;

import com.sescam.radiodiagnostics.common.diagnostics.ProcedureEventPublisher;
import com.sescam.radiodiagnostics.common.diagnostics.ProcedureExecutionService;
import com.sescam.radiodiagnostics.common.diagnostics.ScheduledProcedureGateway;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.diagnostics.internal.InMemoryScheduledProcedureGateway;
import com.sescam.radiodiagnostics.diagnostics.internal.NoOpProcedureEventPublisher;
import com.sescam.radiodiagnostics.diagnostics.repository.ExecutionRepository;
import com.sescam.radiodiagnostics.diagnostics.repository.InMemoryExecutionRepository;
import com.sescam.radiodiagnostics.diagnostics.service.ProcedureExecutionServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration wiring module C7 (sescam-diagnostics) into the server application.
 *
 * <p>Two beans are stand-ins to replace when the modules exist: the bookings of C5 (AAD-05) and the subscribers of
 * the execution event in C4/C5.
 */
@Configuration
public class DiagnosticsWiringConfig {

    @Bean
    public ExecutionRepository executionRepository() {
        return new InMemoryExecutionRepository();
    }

    @Bean
    public ScheduledProcedureGateway scheduledProcedureGateway() {
        return new InMemoryScheduledProcedureGateway();
    }

    @Bean
    public ProcedureEventPublisher procedureEventPublisher() {
        return new NoOpProcedureEventPublisher();
    }

    @Bean
    public ProcedureExecutionService procedureExecutionService(ExecutionRepository executionRepository,
                                                               ScheduledProcedureGateway scheduledProcedureGateway,
                                                               AccessControlService accessControlService,
                                                               AuditService auditService,
                                                               ProcedureEventPublisher procedureEventPublisher) {
        return new ProcedureExecutionServiceImpl(executionRepository, scheduledProcedureGateway, accessControlService,
                auditService, procedureEventPublisher);
    }
}
