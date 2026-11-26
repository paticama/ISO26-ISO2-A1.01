package com.sescam.radiodiagnostics.orders.service;

import com.sescam.radiodiagnostics.common.catalogue.ExamTypeDTO;
import com.sescam.radiodiagnostics.common.catalogue.ExamTypeService;
import com.sescam.radiodiagnostics.common.orders.ConsentAndPreparationDTO;
import com.sescam.radiodiagnostics.common.orders.CreateOrderDTO;
import com.sescam.radiodiagnostics.common.orders.InvalidOrderDataException;
import com.sescam.radiodiagnostics.common.orders.OrderCreatedEvent;
import com.sescam.radiodiagnostics.common.orders.OrderEventPublisher;
import com.sescam.radiodiagnostics.common.orders.OrderResponseDTO;
import com.sescam.radiodiagnostics.common.orders.OrderService;
import com.sescam.radiodiagnostics.common.orders.PatientSafetyGateway;
import com.sescam.radiodiagnostics.common.orders.SafetyAlertException;
import com.sescam.radiodiagnostics.common.orders.SafetyConstraintDTO;
import com.sescam.radiodiagnostics.common.orders.UnauthorizedOrderAccessException;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.orders.domain.DiagnosticOrder;
import com.sescam.radiodiagnostics.orders.internal.SafetyRules;
import com.sescam.radiodiagnostics.orders.repository.OrderRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Realization of UC-PHY-01: Order and Prescribe Diagnostic Procedure.
 */
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ExamTypeService examTypeService;
    private final PatientSafetyGateway patientSafetyGateway;
    private final AccessControlService accessControlService;
    private final AuditService auditService;
    private final OrderEventPublisher eventPublisher;
    private final SafetyRules safetyRules;
    private final Clock clock;

    public OrderServiceImpl(OrderRepository orderRepository,
                            ExamTypeService examTypeService,
                            PatientSafetyGateway patientSafetyGateway,
                            AccessControlService accessControlService,
                            AuditService auditService,
                            OrderEventPublisher eventPublisher) {
        this(orderRepository, examTypeService, patientSafetyGateway, accessControlService, auditService,
                eventPublisher, new SafetyRules(), Clock.systemUTC());
    }

    public OrderServiceImpl(OrderRepository orderRepository,
                            ExamTypeService examTypeService,
                            PatientSafetyGateway patientSafetyGateway,
                            AccessControlService accessControlService,
                            AuditService auditService,
                            OrderEventPublisher eventPublisher,
                            SafetyRules safetyRules,
                            Clock clock) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.examTypeService = Objects.requireNonNull(examTypeService, "examTypeService must not be null");
        this.patientSafetyGateway = Objects.requireNonNull(patientSafetyGateway, "patientSafetyGateway must not be null");
        this.accessControlService = Objects.requireNonNull(accessControlService, "accessControlService must not be null");
        this.auditService = Objects.requireNonNull(auditService, "auditService must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.safetyRules = Objects.requireNonNull(safetyRules, "safetyRules must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public OrderResponseDTO createOrder(CreateOrderDTO request) {
        Objects.requireNonNull(request, "request must not be null");

        // An order must name the prescribing physician, so an unidentified actor is refused like a wrong role.
        if (isBlank(request.actorId()) || !accessControlService.canOrderProcedures(request.actorId(), request.actorRole())) {
            auditRefusal("ORDER_ACCESS_DENIED", request.actorId(), request.patientId(), "Role: " + request.actorRole());
            throw new UnauthorizedOrderAccessException("Role '" + request.actorRole() + "' may not order diagnostic procedures.");
        }

        validate(request);

        ExamTypeDTO exam = examTypeService.getActiveExamType(request.examTypeCode().trim());

        // include: Verify Patient Safety Constraints and Contrast Allergies
        List<SafetyConstraintDTO> incompatible = safetyRules.incompatibleConstraints(
                exam, patientSafetyGateway.getActiveConstraints(request.patientId().trim()));
        Set<String> acknowledged = new HashSet<>(request.acknowledgedConstraintIds());
        boolean allAcknowledged = !incompatible.isEmpty()
                && incompatible.stream().allMatch(c -> c.constraintId() != null && acknowledged.contains(c.constraintId()));
        boolean overridden = allAcknowledged && !isBlank(request.overrideJustification());
        if (!incompatible.isEmpty() && !overridden) {
            auditRefusal("ORDER_SAFETY_ALERT", request.actorId(), request.patientId(),
                    "Exam " + exam.code() + " incompatible with " + incompatible.stream().map(c -> c.type().name()).toList());
            throw new SafetyAlertException("The patient has safety constraints incompatible with " + exam.name()
                    + ". Provide a clinical justification to proceed or cancel the order.", incompatible);
        }

        boolean attachConsent = exam.requiresContrast() || exam.requiresInformedConsent();
        ConsentAndPreparationDTO consent = attachConsent
                ? new ConsentAndPreparationDTO(exam.requiresInformedConsent(), exam.consentTemplateId(),
                exam.preparationGuide(), exam.requiresFasting(), exam.requiresContrast())
                : null;

        Instant now = clock.instant();
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        DiagnosticOrder order = new DiagnosticOrder(
                "ORD-" + suffix, "PHY-" + now.atZone(java.time.ZoneOffset.UTC).getYear() + "-" + suffix,
                request.patientId().trim(), exam.code(), exam.name(), exam.modality(),
                request.clinicalIndication().trim(), request.clinicalJustification().trim(), request.priority(),
                exam.requiresContrast(), consent, overridden ? request.overrideJustification() : null,
                request.actorId(), now);

        // Audit before saving (NFR-11): if the audit store fails, nothing is stored or published,
        // so a retry of the same order does not create a duplicate.
        String action = overridden ? "ORDER_OVERRIDE_APPROVED"
                : attachConsent ? "ORDER_CREATED_CONTRAST_CONSENT_REQUIRED" : "ORDER_CREATED";
        audit(action, request.actorId(), order.getId(),
                "Order " + order.getOrderNumber() + " for patient " + order.getPatientId() + ", exam " + exam.code()
                        + (overridden ? ". Override: " + order.getOverrideJustification() : ""));

        DiagnosticOrder saved;
        try {
            saved = orderRepository.save(order);
        } catch (RuntimeException ex) {
            // Audit entries cannot be changed (NFR-11): record that the audited order was not stored.
            auditRefusal("ORDER_CREATION_FAILED", request.actorId(), order.getId(),
                    "Order " + order.getOrderNumber() + " was not stored: " + ex.getClass().getSimpleName());
            throw ex;
        }
        eventPublisher.publishOrderCreated(new OrderCreatedEvent(saved.getId(), saved.getOrderNumber(),
                saved.getPatientId(), saved.getExamTypeCode(), saved.getPriority(), saved.isRequiresContrast(),
                saved.getOrderedAt()));

        return saved.toDTO();
    }

    private void validate(CreateOrderDTO request) {
        List<String> invalid = new ArrayList<>();
        if (isBlank(request.patientId())) {
            invalid.add("patientId");
        }
        if (isBlank(request.examTypeCode())) {
            invalid.add("examTypeCode");
        }
        if (isBlank(request.clinicalIndication())) {
            invalid.add("clinicalIndication");
        }
        if (isBlank(request.clinicalJustification())) {
            invalid.add("clinicalJustification");
        }
        if (request.priority() == null) {
            invalid.add("priority");
        }
        if (!invalid.isEmpty()) {
            auditRefusal("ORDER_REJECTED", request.actorId(), request.patientId(), "Missing or invalid: " + invalid);
            throw new InvalidOrderDataException("Mandatory order data is missing: " + String.join(", ", invalid) + ".", invalid);
        }
    }

    /** Audit entry of an order about to be stored: if it cannot be recorded, the request fails before saving (NFR-11). */
    private void audit(String action, String actorId, String resourceKey, String details) {
        auditService.recordEvent(new AuditEventDTO(action, actorIdOrUnknown(actorId), resourceKey, null, null,
                details, clock.instant()));
    }

    /**
     * Best-effort audit entry: a refused request (denied, rejected, safety alert) or an order that failed to be stored
     * after it was audited. The outcome is already decided, so a failing audit store must not replace the clear answer
     * (403, 400, 422) or the original storage error.
     */
    private void auditRefusal(String action, String actorId, String resourceKey, String details) {
        try {
            audit(action, actorId, resourceKey, details);
        } catch (RuntimeException ex) {
            // best effort: nothing was stored, the caller still gets the refusal
        }
    }

    private static String actorIdOrUnknown(String actorId) {
        return isBlank(actorId) ? "UNKNOWN" : actorId;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
