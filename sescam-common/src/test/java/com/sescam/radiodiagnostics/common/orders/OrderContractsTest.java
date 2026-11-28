package com.sescam.radiodiagnostics.common.orders;

import com.sescam.radiodiagnostics.common.security.AccessControlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderContractsTest {

    @Test
    @DisplayName("AccessControlService default canOrderProcedures allows only physicians")
    void canOrderProceduresDefault() {
        AccessControlService ac = (actorId, role) -> false;

        assertThat(ac.canOrderProcedures("u1", "PHYSICIAN")).isTrue();
        assertThat(ac.canOrderProcedures("u2", " physician ")).isTrue();
        assertThat(ac.canOrderProcedures("u3", "ADMINISTRATOR")).isFalse();
        assertThat(ac.canOrderProcedures("u4", "ADMISSIONS_STAFF")).isFalse();
        assertThat(ac.canOrderProcedures("u5", null)).isFalse();
        assertThat(ac.canOrderProcedures("u6", "  ")).isFalse();
    }

    @Test
    @DisplayName("SafetyAlertException carries an immutable copy of the constraints")
    void safetyAlertCarriesConstraints() {
        List<SafetyConstraintDTO> source = new ArrayList<>();
        source.add(new SafetyConstraintDTO("c-1", ConstraintType.FERROMAGNETIC_IMPLANT, true, "Pacemaker"));

        SafetyAlertException ex = new SafetyAlertException("Contraindication detected", source);
        source.clear();

        assertThat(ex.getMessage()).isEqualTo("Contraindication detected");
        assertThat(ex.getConstraints()).hasSize(1);
        assertThat(ex.getConstraints().get(0).type()).isEqualTo(ConstraintType.FERROMAGNETIC_IMPLANT);
        assertThatThrownBy(() -> ex.getConstraints().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("InvalidOrderDataException carries an immutable copy of the invalid fields")
    void invalidOrderDataCarriesFields() {
        List<String> source = new ArrayList<>(List.of("clinicalJustification", "priority"));

        InvalidOrderDataException ex = new InvalidOrderDataException("Missing mandatory data", source);
        source.clear();

        assertThat(ex.getInvalidFields()).containsExactly("clinicalJustification", "priority");
        assertThatThrownBy(() -> ex.getInvalidFields().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Order states: a new order starts as PENDING_SCHEDULING, priorities match the use case")
    void enums() {
        assertThat(OrderStatus.values()).contains(OrderStatus.PENDING_SCHEDULING, OrderStatus.CANCELLED);
        assertThat(PriorityLevel.values())
                .containsExactly(PriorityLevel.STAT_URGENT, PriorityLevel.PREFERENTIAL, PriorityLevel.ROUTINE);
    }

    @Test
    @DisplayName("UnauthorizedOrderAccessException preserves its message")
    void unauthorized() {
        assertThat(new UnauthorizedOrderAccessException("Denied").getMessage()).isEqualTo("Denied");
    }
}
