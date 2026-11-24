package com.sescam.radiodiagnostics.schedules.domain;

import com.sescam.radiodiagnostics.common.schedules.ScheduleNotFoundException;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatus;
import com.sescam.radiodiagnostics.common.schedules.SlotUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleTest {

    private static final LocalDateTime DAY = LocalDateTime.of(2026, 12, 1, 9, 0);
    private static final LocalDateTime BEFORE = DAY.minusDays(1);

    private Schedule schedule(int minutes, int capacity) {
        return new Schedule("SCH-1", "ROOM-1", "HGUCR", "MRI", DAY, DAY.plusHours(1), minutes, capacity, Instant.EPOCH);
    }

    @Test
    @DisplayName("Slot ids carry the schedule id and can be parsed back")
    void slotIds() {
        assertThat(Schedule.slotId("SCH-1", DAY)).isEqualTo("SCH-1@20261201T0900");
        assertThat(Schedule.scheduleIdOf("SCH-1@20261201T0900")).contains("SCH-1");
        assertThat(Schedule.scheduleIdOf("@x")).isEmpty();
        assertThat(Schedule.scheduleIdOf(null)).isEmpty();
    }

    @Test
    @DisplayName("Changing the slot duration removes the slots whose grid no longer matches")
    void gridChange() {
        Schedule s = schedule(20, 1);
        Slot second = s.getSlots().get(1); // 09:20-09:40
        s.reserve(second.getId(), second.getVersion(), "BKG-1", BEFORE);

        assertThat(s.appointmentsAffectedBy(DAY, DAY.plusHours(1), 30, 1)).hasSize(1);
        assertThat(s.appointmentsAffectedBy(DAY, DAY.plusHours(1), 20, 1)).isEmpty();
        s.update(DAY, DAY.plusHours(1), 30, 1, "regrid", Instant.EPOCH);
        assertThat(s.getSlots()).hasSize(2);
        assertThat(s.getBookedCount()).isZero();
        assertThat(s.getAppointmentsPendingRescheduling()).hasSize(1);
        assertThat(s.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("Kept slots keep their bookings and get a new version")
    void keptSlots() {
        Schedule s = schedule(30, 2);
        Slot first = s.getSlots().get(0);
        s.reserve(first.getId(), 1, "BKG-1", BEFORE);
        long before = s.findSlot(first.getId()).orElseThrow().getVersion();

        s.update(DAY.minusHours(1), DAY.plusHours(1), 30, 2, null, Instant.EPOCH);

        Slot kept = s.findSlot(first.getId()).orElseThrow();
        assertThat(kept.getBookingReferences()).containsExactly("BKG-1");
        assertThat(kept.getVersion()).isEqualTo(before + 1);
        assertThat(s.getSlots()).hasSize(4);
        assertThat(s.toSummaryDTO().totalCapacity()).isEqualTo(8);
    }

    @Test
    @DisplayName("Started slots cannot be reserved; release of unknown slot only clears pending list")
    void reserveAndRelease() {
        Schedule s = schedule(30, 1);
        Slot first = s.getSlots().get(0);
        assertThatThrownBy(() -> s.reserve(first.getId(), 1, "BKG-1", DAY))
                .isInstanceOf(SlotUnavailableException.class).hasMessageContaining("started");
        assertThatThrownBy(() -> s.reserve("SCH-1@20261201T0915", 1, "BKG-1", BEFORE))
                .isInstanceOf(ScheduleNotFoundException.class);
        assertThat(s.release("SCH-1@20261201T0915", "BKG-1")).isFalse();
        assertThat(s.toDTO(DAY.plusMinutes(31)).slots()).extracting("bookable").containsExactly(false, false);
    }

    @Test
    @DisplayName("Close keeps appointments and lists them; reopen sets the schedule open again")
    void closeAndReopen() {
        Schedule s = schedule(30, 1);
        Slot first = s.getSlots().get(0);
        s.reserve(first.getId(), 1, "BKG-1", BEFORE);
        assertThat(s.appointmentsAffectedByClosing()).hasSize(1);
        s.close("maintenance", Instant.EPOCH);
        assertThat(s.getStatus()).isEqualTo(ScheduleStatus.CLOSED);
        assertThat(s.getBookedCount()).isEqualTo(1);
        s.reopen(null, Instant.EPOCH);
        assertThat(s.isOpen()).isTrue();
        assertThat(s.getLastChangeReason()).isNull();
        assertThat(s.getCreatedAt()).isEqualTo(Instant.EPOCH);
        assertThat(s.getUpdatedAt()).isEqualTo(Instant.EPOCH);
        assertThat(s.getLocationId()).isEqualTo("HGUCR");
        assertThat(first.getCapacity()).isEqualTo(1);
    }
}
