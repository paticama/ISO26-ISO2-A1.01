package com.sescam.radiodiagnostics.orders.internal;

import com.sescam.radiodiagnostics.common.catalogue.ExamTypeDTO;
import com.sescam.radiodiagnostics.common.catalogue.ExamTypeNotFoundException;
import com.sescam.radiodiagnostics.common.orders.OrderCreatedEvent;
import com.sescam.radiodiagnostics.common.orders.PriorityLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StandInsTest {

    @Test
    void catalogueListsOnlyActiveExaminationsAndFindsThemIgnoringCase() {
        InMemoryExamTypeService catalogue = new InMemoryExamTypeService();

        assertThat(catalogue.listActiveExamTypes()).isNotEmpty().allMatch(ExamTypeDTO::active)
                .extracting(ExamTypeDTO::code).contains("XR-CHEST").doesNotContain("XR-OLD");
        assertThat(catalogue.getActiveExamType(" ct-abd-c ").code()).isEqualTo("CT-ABD-C");
    }

    @Test
    void catalogueRejectsUnknownInactiveAndNullCodes() {
        InMemoryExamTypeService catalogue = new InMemoryExamTypeService();

        assertThatThrownBy(() -> catalogue.getActiveExamType("XR-OLD")).isInstanceOf(ExamTypeNotFoundException.class);
        assertThatThrownBy(() -> catalogue.getActiveExamType("???")).isInstanceOf(ExamTypeNotFoundException.class);
        assertThatThrownBy(() -> catalogue.getActiveExamType(null)).isInstanceOf(ExamTypeNotFoundException.class);
    }

    @Test
    void noOpGatewayReportsNoConstraintsAndNoOpPublisherAcceptsEvents() {
        assertThat(new NoOpPatientSafetyGateway().getActiveConstraints("PAT-1")).isEmpty();
        assertThatCode(() -> new NoOpOrderEventPublisher().publishOrderCreated(
                new OrderCreatedEvent("o", "n", "p", "e", PriorityLevel.ROUTINE, false, Instant.now())))
                .doesNotThrowAnyException();
    }
}
