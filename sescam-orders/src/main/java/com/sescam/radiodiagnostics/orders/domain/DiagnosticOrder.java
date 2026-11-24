package com.sescam.radiodiagnostics.orders.domain;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.orders.ConsentAndPreparationDTO;
import com.sescam.radiodiagnostics.common.orders.OrderResponseDTO;
import com.sescam.radiodiagnostics.common.orders.OrderStatus;
import com.sescam.radiodiagnostics.common.orders.PriorityLevel;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity of C4: a diagnostic procedure order (UC-PHY-01). The examination data needed to read the order later (name,
 * modality, consent attachment) is copied at order time, so a later change of the catalogue does not rewrite history.
 */
public class DiagnosticOrder {

    private final String id;
    private final String orderNumber;
    private final String patientId;
    private final String examTypeCode;
    private final String examTypeName;
    private final ModalityType modality;
    private final String clinicalIndication;
    private final String clinicalJustification;
    private final PriorityLevel priority;
    private final boolean requiresContrast;
    private final ConsentAndPreparationDTO consent;
    private final boolean overrideApproved;
    private final String overrideJustification;
    private final String orderedBy;
    private final Instant orderedAt;
    private OrderStatus status;
    private long version;

    /** Creates a new order in {@link OrderStatus#PENDING_SCHEDULING} status, version 1. */
    public DiagnosticOrder(String id, String orderNumber, String patientId,
                           String examTypeCode, String examTypeName, ModalityType modality,
                           String clinicalIndication, String clinicalJustification, PriorityLevel priority,
                           boolean requiresContrast, ConsentAndPreparationDTO consent,
                           String overrideJustification, String orderedBy, Instant orderedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.orderNumber = Objects.requireNonNull(orderNumber, "orderNumber");
        this.patientId = Objects.requireNonNull(patientId, "patientId");
        this.examTypeCode = Objects.requireNonNull(examTypeCode, "examTypeCode");
        this.examTypeName = examTypeName;
        this.modality = modality;
        this.clinicalIndication = Objects.requireNonNull(clinicalIndication, "clinicalIndication");
        this.clinicalJustification = Objects.requireNonNull(clinicalJustification, "clinicalJustification");
        this.priority = Objects.requireNonNull(priority, "priority");
        this.requiresContrast = requiresContrast;
        this.consent = consent;
        this.overrideJustification = overrideJustification == null || overrideJustification.isBlank()
                ? null : overrideJustification.trim();
        this.overrideApproved = this.overrideJustification != null;
        this.orderedBy = Objects.requireNonNull(orderedBy, "orderedBy");
        this.orderedAt = Objects.requireNonNull(orderedAt, "orderedAt");
        this.status = OrderStatus.PENDING_SCHEDULING;
        this.version = 1L;
    }

    public OrderResponseDTO toDTO() {
        return new OrderResponseDTO(id, orderNumber, patientId, examTypeCode, examTypeName, modality,
                clinicalIndication, clinicalJustification, priority, status, requiresContrast, consent,
                overrideApproved, overrideJustification, orderedBy, orderedAt, version);
    }

    public String getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getExamTypeCode() {
        return examTypeCode;
    }

    public PriorityLevel getPriority() {
        return priority;
    }

    public boolean isRequiresContrast() {
        return requiresContrast;
    }

    public boolean isOverrideApproved() {
        return overrideApproved;
    }

    public String getOverrideJustification() {
        return overrideJustification;
    }

    public String getOrderedBy() {
        return orderedBy;
    }

    public Instant getOrderedAt() {
        return orderedAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }
}
