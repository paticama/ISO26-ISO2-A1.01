package com.sescam.radiodiagnostics.schedules.internal;

import com.sescam.radiodiagnostics.common.catalogue.PolicyService;
import com.sescam.radiodiagnostics.common.schedules.CapacityExceededException;
import com.sescam.radiodiagnostics.common.schedules.InvalidScheduleException;
import com.sescam.radiodiagnostics.common.schedules.ScheduleChangedEvent;
import com.sescam.radiodiagnostics.common.schedules.ScheduleOperation;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleRulesAndPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 30, 8, 0);
    private static final LocalDateTime DAY = LocalDateTime.of(2026, 12, 1, 9, 0);

    /** C2 stand-in that returns the fallback, as when the parameter is not configured. */
    private final PolicyService unconfigured = new PolicyService() {
        @Override
        public Optional<String> getPolicyValue(String parameterKey) {
            return Optional.empty();
        }

        @Override
        public int getIntPolicy(String parameterKey, int defaultValue) {
            return defaultValue;
        }
    };

    private final ScheduleRules rules = new ScheduleRules(unconfigured);

    @Test
    @DisplayName("Default limit applies when C2 has no value")
    void defaultLimit() {
        assertThat(rules.maxCapacityPerSlot()).isEqualTo(ScheduleRules.DEFAULT_MAX_CAPACITY_PER_SLOT);
        assertThatCode(() -> rules.validateWindow(DAY, DAY.plusHours(1), 30, 4, NOW)).doesNotThrowAnyException();
        assertThatThrownBy(() -> rules.validateCapacity(5)).isInstanceOf(CapacityExceededException.class);
    }

    @Test
    @DisplayName("A5: every invalid window is reported")
    void invalidWindows() {
        assertThatThrownBy(() -> rules.validateWindow(null, DAY, 30, 1, NOW)).isInstanceOf(InvalidScheduleException.class);
        assertThatThrownBy(() -> rules.validateWindow(DAY, null, 30, 1, NOW)).isInstanceOf(InvalidScheduleException.class);
        assertThatThrownBy(() -> rules.validateWindow(DAY, DAY.minusHours(1), 30, 1, NOW))
                .hasMessageContaining("after its start");
        assertThatThrownBy(() -> rules.validateWindow(DAY, DAY.plusDays(1), 30, 1, NOW))
                .hasMessageContaining("single day");
        assertThatThrownBy(() -> rules.validateWindow(NOW.minusHours(1), NOW.plusHours(1), 30, 1, NOW))
                .hasMessageContaining("past");
        assertThatThrownBy(() -> rules.validateWindow(DAY, DAY.plusHours(1), 4, 1, NOW))
                .hasMessageContaining("at least");
        assertThatThrownBy(() -> rules.validateWindow(DAY, DAY.plusMinutes(50), 30, 1, NOW))
                .hasMessageContaining("multiple");
        assertThatThrownBy(() -> rules.validateWindow(DAY, DAY.plusHours(1), 30, 0, NOW))
                .isInstanceOf(InvalidScheduleException.class);
        assertThatCode(() -> rules.validateWindow(DAY.withHour(23), DAY.plusDays(1).withHour(0), 30, 1, NOW))
                .as("a window ending at midnight is still one day").doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A5: resource, location and modality are mandatory")
    void identity() {
        assertThatThrownBy(() -> rules.validateIdentity(null, "L", "MRI")).hasMessageContaining("Resource");
        assertThatThrownBy(() -> rules.validateIdentity("R", " ", "MRI")).hasMessageContaining("Location");
        assertThatThrownBy(() -> rules.validateIdentity("R", "L", null)).hasMessageContaining("Modality");
        assertThatCode(() -> rules.validateIdentity("R", "L", "MRI")).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{1} may operate: {2}, may view: {3}")
    @CsvSource({
            "staff-1, ADMISSIONS_STAFF, true, true",
            "staff-1, administrative_staff, true, true",
            "staff-1, RECEPTIONIST, true, true",
            "admin-1, SYSTEM_ADMINISTRATOR, false, true",
            "doc-1, PHYSICIAN, false, true",
            "pat-1, PATIENT, false, false",
            "'', ADMISSIONS_STAFF, false, false",
            "staff-1, '', false, false"
    })
    @DisplayName("A6: Admissions Staff operates, administrator and clinicians only consult")
    void accessPolicy(String actor, String role, boolean operate, boolean view) {
        ScheduleAccessPolicy policy = new ScheduleAccessPolicy();
        assertThat(policy.canOperateSchedules(actor, role)).isEqualTo(operate);
        assertThat(policy.canViewSchedules(actor, role)).isEqualTo(view);
    }

    @Test
    @DisplayName("Null actor and role are denied")
    void nullActor() {
        ScheduleAccessPolicy policy = new ScheduleAccessPolicy();
        assertThat(policy.canOperateSchedules(null, null)).isFalse();
        assertThat(policy.canViewSchedules(null, "PHYSICIAN")).isFalse();
    }

    @Test
    @DisplayName("No-op publisher keeps the events until C5 listens to them")
    void noOpPublisher() {
        NoOpScheduleEventPublisher publisher = new NoOpScheduleEventPublisher();
        publisher.publish(new ScheduleChangedEvent("SCH-1", ScheduleOperation.OPEN, ScheduleStatus.OPEN, List.of(),
                "staff-1", Instant.EPOCH));
        assertThat(publisher.getPublished()).hasSize(1);
    }
}
