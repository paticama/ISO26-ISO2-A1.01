package com.sescam.radiodiagnostics.server.diagnostics;

import com.sescam.radiodiagnostics.common.diagnostics.DiagnosticReportingService;
import com.sescam.radiodiagnostics.common.diagnostics.FhirDiagnosticReportGateway;
import com.sescam.radiodiagnostics.common.diagnostics.PacsGateway;
import com.sescam.radiodiagnostics.common.diagnostics.ProcedureEventPublisher;
import com.sescam.radiodiagnostics.common.diagnostics.ProcedureExecutionService;
import com.sescam.radiodiagnostics.common.diagnostics.ReportEventPublisher;
import com.sescam.radiodiagnostics.common.diagnostics.ScheduledProcedureGateway;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.diagnostics.internal.InMemoryPacsGateway;
import com.sescam.radiodiagnostics.diagnostics.internal.InMemoryScheduledProcedureGateway;
import com.sescam.radiodiagnostics.diagnostics.internal.NoOpFhirDiagnosticReportGateway;
import com.sescam.radiodiagnostics.diagnostics.internal.NoOpProcedureEventPublisher;
import com.sescam.radiodiagnostics.diagnostics.internal.NoOpReportEventPublisher;
import com.sescam.radiodiagnostics.diagnostics.repository.ExecutionRepository;
import com.sescam.radiodiagnostics.diagnostics.repository.InMemoryExecutionRepository;
import com.sescam.radiodiagnostics.diagnostics.repository.InMemoryReportRepository;
import com.sescam.radiodiagnostics.diagnostics.repository.ReportRepository;
import com.sescam.radiodiagnostics.diagnostics.service.DiagnosticReportingServiceImpl;
import com.sescam.radiodiagnostics.diagnostics.service.ProcedureExecutionServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration wiring module C7 (sescam-diagnostics) into the server application.
 *
 * <p>Stand-ins are wired for downstream/external modules (C5 bookings, C9 PACS/FHIR, C8 event subscribers)
 * until those integrations are connected.
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

    @Bean
    public ReportRepository reportRepository() {
        return new InMemoryReportRepository();
    }

    @Bean
    public PacsGateway pacsGateway() {
        return new InMemoryPacsGateway();
    }

    @Bean
    public FhirDiagnosticReportGateway fhirDiagnosticReportGateway() {
        return new NoOpFhirDiagnosticReportGateway();
    }

    @Bean
    public ReportEventPublisher reportEventPublisher() {
        return new NoOpReportEventPublisher();
    }

    @Bean
    public DiagnosticReportingService diagnosticReportingService(ReportRepository reportRepository,
                                                                 ExecutionRepository executionRepository,
                                                                 PacsGateway pacsGateway,
                                                                 FhirDiagnosticReportGateway fhirGateway,
                                                                 ReportEventPublisher reportEventPublisher,
                                                                 AccessControlService accessControlService,
                                                                 AuditService auditService) {
        return new DiagnosticReportingServiceImpl(reportRepository, executionRepository, pacsGateway,
                fhirGateway, reportEventPublisher, accessControlService, auditService);
    }
}
