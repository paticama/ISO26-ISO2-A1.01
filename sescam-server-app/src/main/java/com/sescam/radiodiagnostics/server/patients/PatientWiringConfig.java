package com.sescam.radiodiagnostics.server.patients;

import com.sescam.radiodiagnostics.common.patients.EhrGateway;
import com.sescam.radiodiagnostics.common.patients.PatientService;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.patients.ehr.NoOpEhrGateway;
import com.sescam.radiodiagnostics.patients.repository.InMemoryPatientRepository;
import com.sescam.radiodiagnostics.patients.repository.PatientRepository;
import com.sescam.radiodiagnostics.patients.service.PatientServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration wiring module C6 (sescam-patients) into the server application.
 */
@Configuration
public class PatientWiringConfig {

    @Bean
    public PatientRepository patientRepository() {
        return new InMemoryPatientRepository();
    }

    @Bean
    public EhrGateway ehrGateway() {
        return new NoOpEhrGateway();
    }

    @Bean
    public PatientService patientService(PatientRepository patientRepository,
                                         AccessControlService accessControlService,
                                         AuditService auditService,
                                         EhrGateway ehrGateway) {
        return new PatientServiceImpl(patientRepository, accessControlService, auditService, ehrGateway);
    }
}
