package com.sescam.radiodiagnostics.server.integration;

import com.sescam.radiodiagnostics.common.integration.IntegrationService;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.integration.connection.ConnectionProbe;
import com.sescam.radiodiagnostics.integration.connection.NoOpConnectionProbe;
import com.sescam.radiodiagnostics.integration.repository.InMemoryIntegrationRepository;
import com.sescam.radiodiagnostics.integration.repository.IntegrationRepository;
import com.sescam.radiodiagnostics.integration.secret.EncryptedInMemorySecretStore;
import com.sescam.radiodiagnostics.integration.secret.SecretStore;
import com.sescam.radiodiagnostics.integration.service.IntegrationServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires C9 (sescam-integration, UC-ADM-04). Until the real connectors exist the connection check is a
 * no-op that reports "not checked". Access control and audit use the beans of the other modules.
 */
@Configuration
public class IntegrationWiringConfig {

    @Bean
    public IntegrationRepository integrationRepository() {
        return new InMemoryIntegrationRepository();
    }

    @Bean
    public SecretStore integrationSecretStore() {
        return new EncryptedInMemorySecretStore();
    }

    @Bean
    public ConnectionProbe connectionProbe() {
        return new NoOpConnectionProbe();
    }

    @Bean
    public IntegrationService integrationService(IntegrationRepository repository, SecretStore secretStore,
                                                 ConnectionProbe probe, AccessControlService accessControlService,
                                                 AuditService auditService) {
        return new IntegrationServiceImpl(repository, secretStore, probe, accessControlService, auditService);
    }
}
