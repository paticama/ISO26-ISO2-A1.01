package com.sescam.radiodiagnostics.server.catalogue;

import com.sescam.radiodiagnostics.catalogue.internal.DefaultAccessControlService;
import com.sescam.radiodiagnostics.catalogue.internal.NoOpAuditService;
import com.sescam.radiodiagnostics.catalogue.repository.ConfigurationParameterRepository;
import com.sescam.radiodiagnostics.catalogue.repository.InMemoryConfigurationParameterRepository;
import com.sescam.radiodiagnostics.catalogue.service.CatalogueServiceImpl;
import com.sescam.radiodiagnostics.catalogue.service.PolicyServiceImpl;
import com.sescam.radiodiagnostics.common.catalogue.CatalogueService;
import com.sescam.radiodiagnostics.common.catalogue.PolicyService;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogueWiringConfig {

    @Bean
    public ConfigurationParameterRepository configurationParameterRepository() {
        return new InMemoryConfigurationParameterRepository();
    }

    @Bean
    public AccessControlService accessControlService() {
        return new DefaultAccessControlService();
    }

    @Bean
    public AuditService auditService() {
        return new NoOpAuditService();
    }

    @Bean
    public CatalogueService catalogueService(ConfigurationParameterRepository repository,
                                             AccessControlService accessControlService,
                                             AuditService auditService) {
        return new CatalogueServiceImpl(repository, accessControlService, auditService);
    }

    @Bean
    public PolicyService policyService(ConfigurationParameterRepository repository) {
        return new PolicyServiceImpl(repository);
    }
}
