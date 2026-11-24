package com.sescam.radiodiagnostics.common.schedules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleContractsTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 12, 1, 9, 0);

    private static AffectedAppointmentDTO affected() {
        return new AffectedAppointmentDTO("BKG-1", "SCH-1", "SCH-1@0900", START, START.plusMinutes(30), "Slot removed");
    }

    @Test
    @DisplayName("Every exception carries its explicit error code")
    void exceptionCodes() {
        assertThat(new InvalidScheduleException("x").getCode()).isEqualTo(ScheduleErrorCode.INVALID_SCHEDULE);
        assertThat(new CapacityExceededException("x").getCode()).isEqualTo(ScheduleErrorCode.CAPACITY_EXCEEDED);
        assertThat(new UnauthorizedScheduleOperationException("x").getCode())
                .isEqualTo(ScheduleErrorCode.UNAUTHORIZED_OPERATION);
        assertThat(new ScheduleNotFoundException("x").getCode()).isEqualTo(ScheduleErrorCode.SCHEDULE_NOT_FOUND);
        assertThat(new ConcurrentScheduleChangeException("x").getCode())
                .isEqualTo(ScheduleErrorCode.CONCURRENT_MODIFICATION);
        assertThat(new SlotUnavailableException("x").getCode()).isEqualTo(ScheduleErrorCode.SLOT_UNAVAILABLE);
        assertThat(new SlotUnavailableException("msg")).hasMessage("msg");
    }

    @Test
    @DisplayName("A1 and A2 exceptions keep an immutable copy of conflicts and affected appointments")
    void exceptionPayloads() {
        List<ScheduleSummaryDTO> conflicts = new ArrayList<>(List.of(new ScheduleSummaryDTO(
                "SCH-2", "ROOM-1", "HGUCR", "MRI", START, START.plusHours(2), ScheduleStatus.OPEN, 4, 0)));
        ScheduleOverlapException overlap = new ScheduleOverlapException("overlap", conflicts);
        conflicts.clear();
        assertThat(overlap.getCode()).isEqualTo(ScheduleErrorCode.SCHEDULE_OVERLAP);
        assertThat(overlap.getConflictingSchedules()).hasSize(1);
        assertThat(new ScheduleOverlapException("overlap", null).getConflictingSchedules()).isEmpty();

        AppointmentsAffectedException a2 = new AppointmentsAffectedException("affected", List.of(affected()));
        assertThat(a2.getCode()).isEqualTo(ScheduleErrorCode.APPOINTMENTS_AFFECTED);
        assertThat(a2.getAffectedAppointments()).extracting(AffectedAppointmentDTO::bookingReference)
                .containsExactly("BKG-1");
        assertThat(new AppointmentsAffectedException("affected", null).getAffectedAppointments()).isEmpty();
    }

    @Test
    @DisplayName("DTOs replace null lists by empty ones and the slot reports its free capacity")
    void dtoDefaults() {
        ScheduleDTO schedule = new ScheduleDTO("SCH-1", "ROOM-1", "HGUCR", "MRI", START, START.plusHours(1), 30, 2,
                ScheduleStatus.OPEN, 1, null, null, null, Instant.EPOCH);
        assertThat(schedule.slots()).isEmpty();
        assertThat(schedule.appointmentsPendingRescheduling()).isEmpty();

        ScheduleChangeResultDTO result = new ScheduleChangeResultDTO(ScheduleOperation.OPEN, schedule, null, false);
        assertThat(result.affectedAppointments()).isEmpty();

        SlotDTO slot = new SlotDTO("S", "SCH-1", "ROOM-1", "HGUCR", "MRI", START, START.plusMinutes(30), 2, 1, 3, true);
        assertThat(slot.freeCapacity()).isEqualTo(1);
        SlotDTO overbooked = new SlotDTO("S", "SCH-1", "ROOM-1", "HGUCR", "MRI", START, START.plusMinutes(30), 1, 3, 3,
                false);
        assertThat(overbooked.freeCapacity()).isZero();

        assertThat(ScheduleSearchCriteriaDTO.all()).isEqualTo(new ScheduleSearchCriteriaDTO(null, null, null, null));
    }

    @Test
    @DisplayName("ScheduleChangedEvent requires rescheduling only when appointments are affected")
    void changedEvent() {
        ScheduleChangedEvent none = new ScheduleChangedEvent("SCH-1", ScheduleOperation.REOPEN, ScheduleStatus.OPEN,
                null, "staff-1", Instant.EPOCH);
        assertThat(none.requiresRescheduling()).isFalse();
        ScheduleChangedEvent some = new ScheduleChangedEvent("SCH-1", ScheduleOperation.CLOSE, ScheduleStatus.CLOSED,
                List.of(affected()), "staff-1", Instant.EPOCH);
        assertThat(some.requiresRescheduling()).isTrue();
    }
}
