package com.sescam.radiodiagnostics.orders.domain;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.orders.OrderResponseDTO;
import com.sescam.radiodiagnostics.common.orders.OrderStatus;
import com.sescam.radiodiagnostics.common.orders.PriorityLevel;
import com.sescam.radiodiagnostics.orders.repository.InMemoryOrderRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DiagnosticOrderTest {

    private static final Instant NOW = Instant.parse("2026-11-25T10:00:00Z");

    private static DiagnosticOrder order(String id, String override) {
        return new DiagnosticOrder(id, "PHY-2026-" + id, "PAT-1", "XR-CHEST", "Chest X-ray", ModalityType.X_RAY,
                "cough", "rule out pneumonia", PriorityLevel.ROUTINE, false, null, override, "dr-1", NOW);
    }

    @Test
    void newOrderIsPendingSchedulingVersionOne() {
        DiagnosticOrder order = order("A", null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_SCHEDULING);
        assertThat(order.getVersion()).isEqualTo(1L);
        assertThat(order.isOverrideApproved()).isFalse();
        assertThat(order.getOverrideJustification()).isNull();
        assertThat(order.getOrderNumber()).isEqualTo("PHY-2026-A");
        assertThat(order.getOrderedBy()).isEqualTo("dr-1");
        assertThat(order.getOrderedAt()).isEqualTo(NOW);
    }

    @Test
    void justificationMakesTheOverrideApprovedAndBlankDoesNot() {
        assertThat(order("A", " reason ").isOverrideApproved()).isTrue();
        assertThat(order("A", " reason ").getOverrideJustification()).isEqualTo("reason");
        assertThat(order("A", "   ").isOverrideApproved()).isFalse();
    }

    @Test
    void toDtoCopiesTheOrder() {
        OrderResponseDTO dto = order("A", null).toDTO();

        assertThat(dto.orderId()).isEqualTo("A");
        assertThat(dto.patientId()).isEqualTo("PAT-1");
        assertThat(dto.examTypeCode()).isEqualTo("XR-CHEST");
        assertThat(dto.modality()).isEqualTo(ModalityType.X_RAY);
        assertThat(dto.status()).isEqualTo(OrderStatus.PENDING_SCHEDULING);
    }

    @Test
    void mandatoryFieldsAreRequired() {
        assertThatNullPointerException().isThrownBy(() -> new DiagnosticOrder("A", "N", null, "E", "n", null,
                "i", "j", PriorityLevel.ROUTINE, false, null, null, "dr", NOW));
        assertThatNullPointerException().isThrownBy(() -> new DiagnosticOrder("A", "N", "P", "E", "n", null,
                "i", "j", null, false, null, null, "dr", NOW));
    }

    @Test
    void repositoryStoresAndFindsOrders() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        DiagnosticOrder saved = repository.save(order("A", null));

        assertThat(repository.findById("A")).containsSame(saved);
        assertThat(repository.findById("B")).isEmpty();
        assertThat(repository.findById(null)).isEmpty();
        assertThat(repository.count()).isEqualTo(1);
        assertThat(saved.getExamTypeCode()).isEqualTo("XR-CHEST");
        assertThat(saved.getPatientId()).isEqualTo("PAT-1");
        assertThat(saved.getPriority()).isEqualTo(PriorityLevel.ROUTINE);
        assertThat(saved.isRequiresContrast()).isFalse();
    }
}
