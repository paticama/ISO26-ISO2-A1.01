package com.sescam.radiodiagnostics.orders.service;

import com.sescam.radiodiagnostics.common.catalogue.ExamTypeNotFoundException;
import com.sescam.radiodiagnostics.common.catalogue.ExamTypeService;
import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.orders.ConstraintType;
import com.sescam.radiodiagnostics.common.orders.CreateOrderDTO;
import com.sescam.radiodiagnostics.common.orders.InvalidOrderDataException;
import com.sescam.radiodiagnostics.common.orders.OrderCreatedEvent;
import com.sescam.radiodiagnostics.common.orders.OrderEventPublisher;
import com.sescam.radiodiagnostics.common.orders.OrderResponseDTO;
import com.sescam.radiodiagnostics.common.orders.OrderStatus;
import com.sescam.radiodiagnostics.common.orders.PatientSafetyGateway;
import com.sescam.radiodiagnostics.common.orders.PriorityLevel;
import com.sescam.radiodiagnostics.common.orders.SafetyAlertException;
import com.sescam.radiodiagnostics.common.orders.SafetyConstraintDTO;
import com.sescam.radiodiagnostics.common.orders.UnauthorizedOrderAccessException;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.orders.internal.InMemoryExamTypeService;
import com.sescam.radiodiagnostics.orders.repository.InMemoryOrderRepository;
import com.sescam.radiodiagnostics.orders.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-25T10:15:30Z");
    private static final SafetyConstraintDTO IMPLANT =
            new SafetyConstraintDTO("c-1", ConstraintType.FERROMAGNETIC_IMPLANT, true, "Pacemaker");

    private final AccessControlService accessControl = (actorId, role) -> false;
    private OrderRepository repository;
    private PatientSafetyGateway safetyGateway;
    private AuditService auditService;
    private OrderEventPublisher eventPublisher;
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
        safetyGateway = mock(PatientSafetyGateway.class);
        when(safetyGateway.getActiveConstraints(any())).thenReturn(List.of());
        auditService = mock(AuditService.class);
        eventPublisher = mock(OrderEventPublisher.class);
        service = new OrderServiceImpl(repository, new InMemoryExamTypeService(), safetyGateway, accessControl,
                auditService, eventPublisher, new com.sescam.radiodiagnostics.orders.internal.SafetyRules(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static CreateOrderDTO request(String exam, String override) {
        return new CreateOrderDTO("PAT-1", exam, "Persistent cough", "Rule out pneumonia",
                PriorityLevel.PREFERENTIAL, override, "dr-1", "PHYSICIAN");
    }

    private String lastAuditAction() {
        ArgumentCaptor<AuditEventDTO> captor = ArgumentCaptor.forClass(AuditEventDTO.class);
        verify(auditService, org.mockito.Mockito.atLeastOnce()).recordEvent(captor.capture());
        return captor.getAllValues().get(captor.getAllValues().size() - 1).action();
    }

    @Test
    @DisplayName("Main flow: a valid order is stored as PENDING_SCHEDULING, audited and published")
    void createsPendingOrder() {
        OrderResponseDTO order = service.createOrder(request("xr-chest", null));

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_SCHEDULING);
        assertThat(order.version()).isEqualTo(1L);
        assertThat(order.orderNumber()).startsWith("PHY-2026-");
        assertThat(order.examTypeName()).isEqualTo("Chest X-ray");
        assertThat(order.modality()).isEqualTo(ModalityType.X_RAY);
        assertThat(order.priority()).isEqualTo(PriorityLevel.PREFERENTIAL);
        assertThat(order.orderedBy()).isEqualTo("dr-1");
        assertThat(order.orderedAt()).isEqualTo(NOW);
        assertThat(order.consent()).isNull();
        assertThat(order.overrideApproved()).isFalse();
        assertThat(repository.findById(order.orderId())).isPresent();
        assertThat(lastAuditAction()).isEqualTo("ORDER_CREATED");

        ArgumentCaptor<OrderCreatedEvent> event = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publishOrderCreated(event.capture());
        assertThat(event.getValue().orderId()).isEqualTo(order.orderId());
        assertThat(event.getValue().patientId()).isEqualTo("PAT-1");
        assertThat(event.getValue().examTypeCode()).isEqualTo("XR-CHEST");
    }

    @Test
    @DisplayName("Postcondition: every order gets a different tracking number")
    void trackingNumbersAreUnique() {
        OrderResponseDTO first = service.createOrder(request("XR-CHEST", null));
        OrderResponseDTO second = service.createOrder(request("XR-CHEST", null));

        assertThat(first.orderNumber()).isNotEqualTo(second.orderNumber());
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("A6a: contrast and informed consent attach the consent and preparation guide")
    void attachesConsentAndPreparation() {
        OrderResponseDTO order = service.createOrder(request("CT-ABD-C", null));

        assertThat(order.requiresContrast()).isTrue();
        assertThat(order.consent()).isNotNull();
        assertThat(order.consent().informedConsentRequired()).isTrue();
        assertThat(order.consent().consentTemplateId()).isEqualTo("CONSENT-CONTRAST");
        assertThat(order.consent().fastingRequired()).isTrue();
        assertThat(order.consent().preparationGuide()).contains("Fast");
        assertThat(lastAuditAction()).isEqualTo("ORDER_CREATED_CONTRAST_CONSENT_REQUIRED");
    }

    @Test
    @DisplayName("A4a: missing indication, justification or priority is rejected and nothing is stored")
    void rejectsMissingMandatoryData() {
        CreateOrderDTO missing = new CreateOrderDTO(" ", "", null, "  ", null, null, "dr-1", "PHYSICIAN");

        assertThatThrownBy(() -> service.createOrder(missing))
                .isInstanceOfSatisfying(InvalidOrderDataException.class, ex ->
                        assertThat(ex.getInvalidFields()).containsExactly(
                                "patientId", "examTypeCode", "clinicalIndication", "clinicalJustification", "priority"));

        assertThat(repository.count()).isZero();
        assertThat(lastAuditAction()).isEqualTo("ORDER_REJECTED");
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Only physicians can order: other roles are denied and the attempt is audited")
    void deniesOtherRoles() {
        CreateOrderDTO admin = new CreateOrderDTO("PAT-1", "XR-CHEST", "i", "j", PriorityLevel.ROUTINE, null,
                "admin-1", "ADMINISTRATOR");

        assertThatThrownBy(() -> service.createOrder(admin)).isInstanceOf(UnauthorizedOrderAccessException.class);

        assertThat(repository.count()).isZero();
        assertThat(lastAuditAction()).isEqualTo("ORDER_ACCESS_DENIED");
        verifyNoInteractions(safetyGateway, eventPublisher);
    }

    @Test
    @DisplayName("A physician role without an actor id is refused: every order names its prescriber")
    void deniesUnidentifiedActor() {
        AccessControlService anyoneAllowed = new AccessControlService() {
            @Override
            public boolean canManageConfiguration(String actorId, String role) {
                return false;
            }

            @Override
            public boolean canOrderProcedures(String actorId, String role) {
                return true;
            }
        };
        OrderServiceImpl lenient = new OrderServiceImpl(repository, new InMemoryExamTypeService(), safetyGateway,
                anyoneAllowed, auditService, eventPublisher);

        assertThatThrownBy(() -> lenient.createOrder(new CreateOrderDTO("PAT-1", "XR-CHEST", "i", "j",
                PriorityLevel.ROUTINE, null, " ", "PHYSICIAN")))
                .isInstanceOf(UnauthorizedOrderAccessException.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("Unknown or inactive examination types are not orderable")
    void rejectsUnknownExam() {
        assertThatThrownBy(() -> service.createOrder(request("NOPE", null)))
                .isInstanceOf(ExamTypeNotFoundException.class);
        assertThatThrownBy(() -> service.createOrder(request("XR-OLD", null)))
                .isInstanceOf(ExamTypeNotFoundException.class);

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("A6b: a safety constraint without override raises an alert and stores nothing")
    void safetyAlertWithoutOverride() {
        when(safetyGateway.getActiveConstraints("PAT-1")).thenReturn(List.of(IMPLANT));

        assertThatThrownBy(() -> service.createOrder(request("MR-BRAIN", "  ")))
                .isInstanceOfSatisfying(SafetyAlertException.class, ex -> {
                    assertThat(ex.getConstraints()).containsExactly(IMPLANT);
                    assertThat(ex.getMessage()).contains("Brain MRI");
                });

        assertThat(repository.count()).isZero();
        assertThat(lastAuditAction()).isEqualTo("ORDER_SAFETY_ALERT");
        verify(eventPublisher, never()).publishOrderCreated(any());
    }

    @Test
    @DisplayName("A6b: with a justification the order is stored with the override and the override is audited")
    void safetyOverrideIsStoredAndAudited() {
        when(safetyGateway.getActiveConstraints("PAT-1")).thenReturn(List.of(IMPLANT));

        OrderResponseDTO order = service.createOrder(request("MR-BRAIN", "  MR-conditional pacemaker, cardiology OK "));

        assertThat(order.overrideApproved()).isTrue();
        assertThat(order.overrideJustification()).isEqualTo("MR-conditional pacemaker, cardiology OK");
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_SCHEDULING);
        assertThat(lastAuditAction()).isEqualTo("ORDER_OVERRIDE_APPROVED");
        verify(eventPublisher).publishOrderCreated(any());
    }

    @Test
    @DisplayName("A constraint that does not affect the examination does not block the order")
    void compatibleConstraintIsIgnored() {
        when(safetyGateway.getActiveConstraints("PAT-1")).thenReturn(List.of(IMPLANT));

        OrderResponseDTO order = service.createOrder(request("US-ABD", null));

        assertThat(order.overrideApproved()).isFalse();
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_SCHEDULING);
    }

    @Test
    @DisplayName("An unused override justification is not recorded when there is no constraint")
    void overrideWithoutConstraintIsIgnored() {
        OrderResponseDTO order = service.createOrder(request("XR-CHEST", "just in case"));

        assertThat(order.overrideApproved()).isFalse();
        assertThat(order.overrideJustification()).isNull();
    }

    @Test
    @DisplayName("A blank actor is audited as UNKNOWN, and a failing audit store does not hide a refusal")
    void refusalsSurviveAnAuditFailure() {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Audit event must have an actor"))
                .when(auditService).recordEvent(any());
        CreateOrderDTO anonymous = new CreateOrderDTO("PAT-1", "XR-CHEST", "i", "j", PriorityLevel.ROUTINE, null, "", "");

        assertThatThrownBy(() -> service.createOrder(anonymous)).isInstanceOf(UnauthorizedOrderAccessException.class);
        assertThatThrownBy(() -> service.createOrder(new CreateOrderDTO(null, null, null, null, null, null, "dr-1", "PHYSICIAN")))
                .isInstanceOf(InvalidOrderDataException.class);

        ArgumentCaptor<AuditEventDTO> captor = ArgumentCaptor.forClass(AuditEventDTO.class);
        verify(auditService, org.mockito.Mockito.times(2)).recordEvent(captor.capture());
        assertThat(captor.getAllValues().get(0).actorId()).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("If the audit entry of a stored order cannot be recorded, the request fails")
    void auditFailureOnSuccessPropagates() {
        org.mockito.Mockito.doThrow(new IllegalStateException("audit store down"))
                .when(auditService).recordEvent(any());

        assertThatThrownBy(() -> service.createOrder(request("XR-CHEST", null)))
                .isInstanceOf(IllegalStateException.class);
        verify(eventPublisher, never()).publishOrderCreated(any());
    }

    @Test
    @DisplayName("Constructor and request reject null collaborators")
    void nullChecks() {
        assertThatNullPointerException().isThrownBy(() -> service.createOrder(null));
        assertThatNullPointerException().isThrownBy(() -> new OrderServiceImpl(null, mock(ExamTypeService.class),
                safetyGateway, accessControl, auditService, eventPublisher));
        assertThatNullPointerException().isThrownBy(() -> new OrderServiceImpl(repository, null,
                safetyGateway, accessControl, auditService, eventPublisher));
        assertThatNullPointerException().isThrownBy(() -> new OrderServiceImpl(repository, mock(ExamTypeService.class),
                null, accessControl, auditService, eventPublisher));
        assertThatNullPointerException().isThrownBy(() -> new OrderServiceImpl(repository, mock(ExamTypeService.class),
                safetyGateway, null, auditService, eventPublisher));
        assertThatNullPointerException().isThrownBy(() -> new OrderServiceImpl(repository, mock(ExamTypeService.class),
                safetyGateway, accessControl, null, eventPublisher));
        assertThatNullPointerException().isThrownBy(() -> new OrderServiceImpl(repository, mock(ExamTypeService.class),
                safetyGateway, accessControl, auditService, null));
    }
}
