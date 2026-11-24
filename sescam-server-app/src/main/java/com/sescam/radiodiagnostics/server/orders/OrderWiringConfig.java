package com.sescam.radiodiagnostics.server.orders;

import com.sescam.radiodiagnostics.common.catalogue.ExamTypeService;
import com.sescam.radiodiagnostics.common.orders.OrderEventPublisher;
import com.sescam.radiodiagnostics.common.orders.OrderService;
import com.sescam.radiodiagnostics.common.orders.PatientSafetyGateway;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.orders.internal.InMemoryExamTypeService;
import com.sescam.radiodiagnostics.orders.internal.NoOpOrderEventPublisher;
import com.sescam.radiodiagnostics.orders.internal.NoOpPatientSafetyGateway;
import com.sescam.radiodiagnostics.orders.repository.InMemoryOrderRepository;
import com.sescam.radiodiagnostics.orders.repository.OrderRepository;
import com.sescam.radiodiagnostics.orders.service.OrderServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration wiring module C4 (sescam-orders) into the server application.
 *
 * <p>Three beans are stand-ins for modules that do not exist yet and must be replaced when they do: the examination
 * catalogue (C2), the EHR safety connector (C9) and the order subscriber (C5).
 */
@Configuration
public class OrderWiringConfig {

    @Bean
    public OrderRepository orderRepository() {
        return new InMemoryOrderRepository();
    }

    @Bean
    public ExamTypeService examTypeService() {
        return new InMemoryExamTypeService();
    }

    @Bean
    public PatientSafetyGateway patientSafetyGateway() {
        return new NoOpPatientSafetyGateway();
    }

    @Bean
    public OrderEventPublisher orderEventPublisher() {
        return new NoOpOrderEventPublisher();
    }

    @Bean
    public OrderService orderService(OrderRepository orderRepository,
                                     ExamTypeService examTypeService,
                                     PatientSafetyGateway patientSafetyGateway,
                                     AccessControlService accessControlService,
                                     AuditService auditService,
                                     OrderEventPublisher orderEventPublisher) {
        return new OrderServiceImpl(orderRepository, examTypeService, patientSafetyGateway, accessControlService,
                auditService, orderEventPublisher);
    }
}
