package es.uclm.esi.sescam.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import es.uclm.esi.sescam.security.api.AccessControlService;
import es.uclm.esi.sescam.security.api.AuditService;
import es.uclm.esi.sescam.server.security.RoleAdministrationController;

@SpringBootTest
class SescamServerApplicationTest {

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private RoleAdministrationController controller;

    @Test
    void contextWiresTheSecurityModuleBehindItsInterfaces() {
        assertThat(accessControlService).isNotNull();
        assertThat(auditService).isNotNull();
        assertThat(controller).isNotNull();
    }
}
