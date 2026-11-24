package com.sescam.radiodiagnostics.schedules.service;

import com.sescam.radiodiagnostics.common.catalogue.PolicyService;
import com.sescam.radiodiagnostics.common.schedules.AffectedAppointmentDTO;
import com.sescam.radiodiagnostics.common.schedules.AppointmentsAffectedException;
import com.sescam.radiodiagnostics.common.schedules.CapacityExceededException;
import com.sescam.radiodiagnostics.common.schedules.ConcurrentScheduleChangeException;
import com.sescam.radiodiagnostics.common.schedules.InvalidScheduleException;
import com.sescam.radiodiagnostics.common.schedules.OpenScheduleRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleChangeResultDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleChangedEvent;
import com.sescam.radiodiagnostics.common.schedules.ScheduleDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleErrorCode;
import com.sescam.radiodiagnostics.common.schedules.ScheduleEventPublisher;
import com.sescam.radiodiagnostics.common.schedules.ScheduleNotFoundException;
import com.sescam.radiodiagnostics.common.schedules.ScheduleOperation;
import com.sescam.radiodiagnostics.common.schedules.ScheduleOverlapException;
import com.sescam.radiodiagnostics.common.schedules.ScheduleSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatus;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatusChangeRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleSummaryDTO;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import com.sescam.radiodiagnostics.common.schedules.SlotUnavailableException;
import com.sescam.radiodiagnostics.common.schedules.UnauthorizedScheduleOperationException;
import com.sescam.radiodiagnostics.common.schedules.UpdateScheduleRequestDTO;
import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.schedules.internal.ScheduleAccessPolicy;
import com.sescam.radiodiagnostics.schedules.internal.ScheduleRules;
import com.sescam.radiodiagnostics.schedules.repository.InMemoryScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleServiceImplTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 30, 8, 0);
    private static final LocalDateTime DAY = LocalDateTime.of(2026, 12, 1, 9, 0);
    private static final String STAFF = "staff-1";
    private static final String STAFF_ROLE = "ADMISSIONS_STAFF";

    private AuditService auditService;
    private ScheduleEventPublisher publisher;
    private PolicyService policyService;
    private ScheduleServiceImpl service;
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        publisher = mock(ScheduleEventPublisher.class);
        policyService = mock(PolicyService.class);
        when(policyService.getIntPolicy(eq(ScheduleRules.MAX_CAPACITY_PER_SLOT_KEY), anyInt())).thenReturn(4);
        clock = new MutableClock(NOW);
        service = new ScheduleServiceImpl(new InMemoryScheduleRepository(), new ScheduleRules(policyService),
                new ScheduleAccessPolicy(), auditService, publisher, clock);
    }

    private OpenScheduleRequestDTO openRequest(String resource, LocalDateTime start, LocalDateTime end) {
        return new OpenScheduleRequestDTO(resource, "HGUCR", "mri", start, end, 30, 1, STAFF, STAFF_ROLE);
    }

    /** 09:00-11:00, 4 slots of 30 minutes, 1 place each. */
    private ScheduleDTO openDefault() {
        return service.openSchedule(openRequest("MRI-ROOM-1", DAY, DAY.plusHours(2))).schedule();
    }

    private UpdateScheduleRequestDTO update(ScheduleDTO s, LocalDateTime start, LocalDateTime end, int minutes,
                                            int capacity, boolean confirm) {
        return new UpdateScheduleRequestDTO(s.scheduleId(), start, end, minutes, capacity, s.version(), confirm,
                "Maintenance", STAFF, STAFF_ROLE);
    }

    private ScheduleStatusChangeRequestDTO statusChange(String id, long version, boolean confirm, String reason) {
        return new ScheduleStatusChangeRequestDTO(id, version, confirm, reason, STAFF, STAFF_ROLE);
    }

    private List<String> auditActions() {
        ArgumentCaptor<AuditEventDTO> captor = ArgumentCaptor.forClass(AuditEventDTO.class);
        verify(auditService, atLeastOnce()).recordEvent(captor.capture());
        return captor.getAllValues().stream().map(AuditEventDTO::action).toList();
    }

    @Nested
    @DisplayName("Main success scenario")
    class MainFlow {

        @Test
        @DisplayName("AC1: Admissions Staff opens a schedule; slots are generated, audited and published")
        void openSchedule() {
            ScheduleChangeResultDTO result = service.openSchedule(openRequest("MRI-ROOM-1", DAY, DAY.plusHours(2)));

            ScheduleDTO s = result.schedule();
            assertThat(result.operation()).isEqualTo(ScheduleOperation.OPEN);
            assertThat(result.reschedulingRequired()).isFalse();
            assertThat(s.status()).isEqualTo(ScheduleStatus.OPEN);
            assertThat(s.modality()).isEqualTo("MRI");
            assertThat(s.version()).isEqualTo(1);
            assertThat(s.slots()).hasSize(4).allMatch(SlotDTO::bookable);
            assertThat(s.slots().get(0).slotId()).isEqualTo(s.scheduleId() + "@20261201T0900");
            assertThat(auditActions()).containsExactly("SCHEDULE_OPENED");
            ArgumentCaptor<ScheduleChangedEvent> event = ArgumentCaptor.forClass(ScheduleChangedEvent.class);
            verify(publisher).publish(event.capture());
            assertThat(event.getValue().operation()).isEqualTo(ScheduleOperation.OPEN);
        }

        @Test
        @DisplayName("Steps 1-2: search by date, resource and location, and consult one schedule")
        void searchAndGet() {
            ScheduleDTO s = openDefault();
            service.openSchedule(openRequest("CT-ROOM-1", DAY.plusDays(1), DAY.plusDays(1).plusHours(1)));

            List<ScheduleSummaryDTO> byDate = service.searchSchedules(
                    new ScheduleSearchCriteriaDTO(LocalDate.of(2026, 12, 1), null, null, null), STAFF, STAFF_ROLE);
            assertThat(byDate).extracting(ScheduleSummaryDTO::scheduleId).containsExactly(s.scheduleId());
            assertThat(service.searchSchedules(null, STAFF, STAFF_ROLE)).hasSize(2);
            assertThat(service.searchSchedules(new ScheduleSearchCriteriaDTO(null, "ct-room-1", "hgucr", null),
                    STAFF, STAFF_ROLE)).hasSize(1);
            assertThat(service.getSchedule(s.scheduleId(), STAFF, STAFF_ROLE).slots()).hasSize(4);
        }

        @Test
        @DisplayName("AC1: update extends the window without affecting appointments")
        void updateWithoutImpact() {
            ScheduleDTO s = openDefault();
            SlotDTO first = s.slots().get(0);
            service.reserveSlotAtomically(first.slotId(), first.version(), "BKG-1");

            ScheduleChangeResultDTO result = service.updateSchedule(
                    update(service.getSchedule(s.scheduleId(), STAFF, STAFF_ROLE), DAY, DAY.plusHours(3), 30, 2, false));

            assertThat(result.affectedAppointments()).isEmpty();
            assertThat(result.schedule().slots()).hasSize(6);
            assertThat(result.schedule().slots().get(0).bookedCount()).isEqualTo(1);
            assertThat(result.schedule().capacityPerSlot()).isEqualTo(2);
            assertThat(result.schedule().version()).isEqualTo(2);
            assertThat(auditActions()).contains("SCHEDULE_UPDATED");
        }

        @Test
        @DisplayName("Postcondition 3: confirmed availability is exposed to the booking workflow")
        void compatibleSlots() {
            ScheduleDTO s = openDefault();
            service.openSchedule(new OpenScheduleRequestDTO("CT-ROOM-1", "HGUCR", "CT", DAY, DAY.plusHours(1), 60, 1,
                    STAFF, STAFF_ROLE));

            assertThat(service.findCompatibleSlots("MRI", DAY, DAY.plusHours(1))).hasSize(2)
                    .allMatch(slot -> slot.scheduleId().equals(s.scheduleId()));
            assertThat(service.findCompatibleSlots(null, null, null)).hasSize(5);
            assertThat(service.findCompatibleSlots(" ", DAY.plusHours(1), null)).hasSize(2);
        }
    }

    @Nested
    @DisplayName("A1. Schedule overlap")
    class Overlap {

        @Test
        @DisplayName("AC2: an overlapping schedule on the same resource is rejected with the conflicts")
        void overlapRejected() {
            ScheduleDTO existing = openDefault();

            assertThatThrownBy(() -> service.openSchedule(
                    openRequest("MRI-ROOM-1", DAY.plusHours(1), DAY.plusHours(3))))
                    .isInstanceOfSatisfying(ScheduleOverlapException.class, ex -> {
                        assertThat(ex.getCode()).isEqualTo(ScheduleErrorCode.SCHEDULE_OVERLAP);
                        assertThat(ex.getConflictingSchedules()).extracting(ScheduleSummaryDTO::scheduleId)
                                .containsExactly(existing.scheduleId());
                    });
            assertThat(auditActions()).contains("SCHEDULE_CHANGE_REJECTED");
        }

        @Test
        @DisplayName("Adjacent windows and other resources do not overlap")
        void noFalseOverlap() {
            openDefault();
            service.openSchedule(openRequest("MRI-ROOM-1", DAY.plusHours(2), DAY.plusHours(3)));
            service.openSchedule(openRequest("MRI-ROOM-2", DAY, DAY.plusHours(2)));
            assertThat(service.searchSchedules(null, STAFF, STAFF_ROLE)).hasSize(3);
        }

        @Test
        @DisplayName("An update cannot move the window over another open schedule")
        void updateOverlap() {
            ScheduleDTO first = openDefault();
            service.openSchedule(openRequest("MRI-ROOM-1", DAY.plusHours(3), DAY.plusHours(4)));
            assertThatThrownBy(() -> service.updateSchedule(update(first, DAY, DAY.plusHours(4), 30, 1, false)))
                    .isInstanceOf(ScheduleOverlapException.class);
        }
    }

    @Nested
    @DisplayName("A2. Existing appointments affected")
    class Affected {

        @Test
        @DisplayName("AC3: affected appointments are listed and nothing is saved until confirmed")
        void listedBeforeCommit() {
            ScheduleDTO s = openDefault();
            SlotDTO last = s.slots().get(3);
            service.reserveSlotAtomically(last.slotId(), last.version(), "BKG-LATE");
            ScheduleDTO current = service.getSchedule(s.scheduleId(), STAFF, STAFF_ROLE);

            assertThatThrownBy(() -> service.updateSchedule(update(current, DAY, DAY.plusHours(1), 30, 1, false)))
                    .isInstanceOfSatisfying(AppointmentsAffectedException.class, ex ->
                            assertThat(ex.getAffectedAppointments()).extracting(AffectedAppointmentDTO::bookingReference)
                                    .containsExactly("BKG-LATE"));
            ScheduleDTO unchanged = service.getSchedule(s.scheduleId(), STAFF, STAFF_ROLE);
            assertThat(unchanged.slots()).hasSize(4);
            assertThat(unchanged.version()).isEqualTo(current.version());
        }

        @Test
        @DisplayName("Postcondition 2: once confirmed, affected appointments are preserved and handed to admissions")
        void confirmedChangePreservesAppointments() {
            ScheduleDTO s = openDefault();
            SlotDTO last = s.slots().get(3);
            service.reserveSlotAtomically(last.slotId(), last.version(), "BKG-LATE");
            ScheduleDTO current = service.getSchedule(s.scheduleId(), STAFF, STAFF_ROLE);

            ScheduleChangeResultDTO result = service.updateSchedule(update(current, DAY, DAY.plusHours(1), 30, 1, true));

            assertThat(result.reschedulingRequired()).isTrue();
            assertThat(result.schedule().slots()).hasSize(2);
            assertThat(result.schedule().appointmentsPendingRescheduling())
                    .extracting(AffectedAppointmentDTO::bookingReference).containsExactly("BKG-LATE");
            ArgumentCaptor<ScheduleChangedEvent> event = ArgumentCaptor.forClass(ScheduleChangedEvent.class);
            verify(publisher, atLeastOnce()).publish(event.capture());
            assertThat(event.getValue().requiresRescheduling()).isTrue();

            // Admissions reschedules the booking and gives the place back: it leaves the pending list.
            service.releaseSlot(last.slotId(), "BKG-LATE");
            assertThat(service.getSchedule(s.scheduleId(), STAFF, STAFF_ROLE).appointmentsPendingRescheduling())
                    .isEmpty();
        }

        @Test
        @DisplayName("Capacity reduction affects the latest reservations of a slot")
        void capacityReduction() {
            ScheduleDTO s = service.openSchedule(new OpenScheduleRequestDTO("MRI-ROOM-1", "HGUCR", "MRI", DAY,
                    DAY.plusHours(1), 30, 2, STAFF, STAFF_ROLE)).schedule();
            SlotDTO slot = s.slots().get(0);
            SlotDTO afterFirst = service.reserveSlotAtomically(slot.slotId(), slot.version(), "BKG-1");
            service.reserveSlotAtomically(slot.slotId(), afterFirst.version(), "BKG-2");
            ScheduleDTO current = service.getSchedule(s.scheduleId(), STAFF, STAFF_ROLE);

            assertThatThrownBy(() -> service.updateSchedule(update(current, DAY, DAY.plusHours(1), 30, 1, false)))
                    .isInstanceOfSatisfying(AppointmentsAffectedException.class, ex ->
                            assertThat(ex.getAffectedAppointments()).extracting(AffectedAppointmentDTO::bookingReference)
                                    .containsExactly("BKG-2"));
        }
    }

    @Nested
    @DisplayName("A3. Close schedule")
    class Close {

        @Test
        @DisplayName("AC4: a closed schedule accepts no new bookings and keeps existing appointments")
        void closePreventsBookings() {
            ScheduleDTO s = openDefault();
            SlotDTO first = s.slots().get(0);
            service.reserveSlotAtomically(first.slotId(), first.version(), "BKG-1");
            ScheduleDTO current = service.getSchedule(s.scheduleId(), STAFF, STAFF_ROLE);

            assertThatThrownBy(() -> service.closeSchedule(statusChange(s.scheduleId(), current.version(), false,
                    "Scanner maintenance"))).isInstanceOf(AppointmentsAffectedException.class);

            ScheduleChangeResultDTO result = service.closeSchedule(statusChange(s.scheduleId(), current.version(), true,
                    "Scanner maintenance"));

            assertThat(result.schedule().status()).isEqualTo(ScheduleStatus.CLOSED);
            assertThat(result.schedule().slots().get(0).bookedCount()).isEqualTo(1);
            assertThat(result.schedule().slots()).noneMatch(SlotDTO::bookable);
            assertThat(result.affectedAppointments()).hasSize(1);
            assertThat(result.schedule().lastChangeReason()).isEqualTo("Scanner maintenance");
            SlotDTO second = result.schedule().slots().get(1);
            assertThatThrownBy(() -> service.reserveSlotAtomically(second.slotId(), second.version(), "BKG-2"))
                    .isInstanceOf(SlotUnavailableException.class);
            assertThat(service.findCompatibleSlots("MRI", null, null)).isEmpty();
            assertThat(auditActions()).contains("SCHEDULE_CLOSED");
        }

        @Test
        @DisplayName("Closing needs a reason, an open schedule and the current version")
        void closeValidation() {
            ScheduleDTO s = openDefault();
            assertThatThrownBy(() -> service.closeSchedule(statusChange(s.scheduleId(), 1, false, " ")))
                    .isInstanceOf(InvalidScheduleException.class);
            assertThatThrownBy(() -> service.closeSchedule(statusChange(s.scheduleId(), 7, false, "x")))
                    .isInstanceOf(ConcurrentScheduleChangeException.class);
            service.closeSchedule(statusChange(s.scheduleId(), 1, false, "Holiday"));
            assertThatThrownBy(() -> service.closeSchedule(statusChange(s.scheduleId(), 2, false, "Again")))
                    .isInstanceOf(InvalidScheduleException.class);
            assertThatThrownBy(() -> service.updateSchedule(new UpdateScheduleRequestDTO(s.scheduleId(), DAY,
                    DAY.plusHours(1), 30, 1, 2, false, null, STAFF, STAFF_ROLE)))
                    .isInstanceOf(InvalidScheduleException.class).hasMessageContaining("closed");
        }
    }

    @Nested
    @DisplayName("A4. Reopen schedule")
    class Reopen {

        @Test
        @DisplayName("AC5: reopened schedule passes conflict and capacity checks before slots are bookable")
        void reopen() {
            ScheduleDTO s = openDefault();
            service.closeSchedule(statusChange(s.scheduleId(), 1, false, "Holiday"));

            ScheduleChangeResultDTO result = service.reopenSchedule(statusChange(s.scheduleId(), 2, false, "Back"));

            assertThat(result.schedule().status()).isEqualTo(ScheduleStatus.OPEN);
            assertThat(result.schedule().slots()).allMatch(SlotDTO::bookable);
            assertThat(auditActions()).contains("SCHEDULE_REOPENED");
        }

        @Test
        @DisplayName("Reopen is rejected when another schedule took the resource meanwhile")
        void reopenConflict() {
            ScheduleDTO s = openDefault();
            service.closeSchedule(statusChange(s.scheduleId(), 1, false, "Holiday"));
            service.openSchedule(openRequest("MRI-ROOM-1", DAY, DAY.plusHours(1)));

            assertThatThrownBy(() -> service.reopenSchedule(statusChange(s.scheduleId(), 2, false, null)))
                    .isInstanceOf(ScheduleOverlapException.class);
        }

        @Test
        @DisplayName("Reopen is rejected when the configured capacity limit was lowered")
        void reopenCapacity() {
            ScheduleDTO s = service.openSchedule(new OpenScheduleRequestDTO("MRI-ROOM-1", "HGUCR", "MRI", DAY,
                    DAY.plusHours(1), 30, 3, STAFF, STAFF_ROLE)).schedule();
            service.closeSchedule(statusChange(s.scheduleId(), 1, false, "Holiday"));
            when(policyService.getIntPolicy(eq(ScheduleRules.MAX_CAPACITY_PER_SLOT_KEY), anyInt())).thenReturn(2);

            assertThatThrownBy(() -> service.reopenSchedule(statusChange(s.scheduleId(), 2, false, null)))
                    .isInstanceOf(CapacityExceededException.class);
        }

        @Test
        @DisplayName("An open schedule cannot be reopened")
        void reopenOpen() {
            ScheduleDTO s = openDefault();
            assertThatThrownBy(() -> service.reopenSchedule(statusChange(s.scheduleId(), 1, false, null)))
                    .isInstanceOf(InvalidScheduleException.class);
        }

        @Test
        @DisplayName("A schedule that already ended cannot be reopened")
        void reopenEnded() {
            ScheduleDTO s = openDefault();
            service.closeSchedule(statusChange(s.scheduleId(), 1, false, "Holiday"));
            clock.set(NOW.plusDays(2));
            assertThatThrownBy(() -> service.reopenSchedule(statusChange(s.scheduleId(), 2, false, null)))
                    .isInstanceOf(InvalidScheduleException.class).hasMessageContaining("ended");
        }
    }

    /** Clock the tests can move forward. */
    static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(LocalDateTime start) {
            set(start);
        }

        void set(LocalDateTime time) {
            instant = time.atZone(MADRID).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return MADRID;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    @Nested
    @DisplayName("A5. Invalid configuration")
    class Invalid {

        @Test
        @DisplayName("Invalid dates, resource or capacity are reported and nothing is saved")
        void invalidRejected() {
            assertThatThrownBy(() -> service.openSchedule(openRequest(" ", DAY, DAY.plusHours(1))))
                    .isInstanceOf(InvalidScheduleException.class);
            assertThatThrownBy(() -> service.openSchedule(openRequest("R", DAY, DAY)))
                    .isInstanceOf(InvalidScheduleException.class);
            assertThatThrownBy(() -> service.openSchedule(new OpenScheduleRequestDTO("R", "L", "MRI", DAY,
                    DAY.plusHours(1), 30, 0, STAFF, STAFF_ROLE))).isInstanceOf(InvalidScheduleException.class);
            assertThat(service.searchSchedules(null, STAFF, STAFF_ROLE)).isEmpty();
            verify(publisher, never()).publish(any());
        }

        @Test
        @DisplayName("AC2: capacity above the limit configured by the System Administrator is rejected")
        void capacityExceeded() {
            assertThatThrownBy(() -> service.openSchedule(new OpenScheduleRequestDTO("R", "L", "MRI", DAY,
                    DAY.plusHours(1), 30, 5, STAFF, STAFF_ROLE)))
                    .isInstanceOfSatisfying(CapacityExceededException.class,
                            ex -> assertThat(ex.getCode()).isEqualTo(ScheduleErrorCode.CAPACITY_EXCEEDED));
        }

        @Test
        @DisplayName("Unknown or blank schedule ids and stale versions are reported")
        void lookups() {
            assertThatThrownBy(() -> service.getSchedule("SCH-NOPE", STAFF, STAFF_ROLE))
                    .isInstanceOf(ScheduleNotFoundException.class);
            assertThatThrownBy(() -> service.getSchedule(" ", STAFF, STAFF_ROLE))
                    .isInstanceOf(InvalidScheduleException.class);
            ScheduleDTO s = openDefault();
            assertThatThrownBy(() -> service.updateSchedule(new UpdateScheduleRequestDTO(s.scheduleId(), DAY,
                    DAY.plusHours(1), 30, 1, 99, false, null, STAFF, STAFF_ROLE)))
                    .isInstanceOf(ConcurrentScheduleChangeException.class);
        }
    }

    @Nested
    @DisplayName("A6. Unauthorized administration")
    class Unauthorized {

        @Test
        @DisplayName("AC6: System Administrator can consult but not operate schedules; denial is audited")
        void adminCannotOperate() {
            ScheduleDTO s = openDefault();
            assertThat(service.getSchedule(s.scheduleId(), "admin-1", "SYSTEM_ADMINISTRATOR")).isNotNull();

            assertThatThrownBy(() -> service.openSchedule(new OpenScheduleRequestDTO("R", "L", "MRI", DAY,
                    DAY.plusHours(1), 30, 1, "admin-1", "SYSTEM_ADMINISTRATOR")))
                    .isInstanceOfSatisfying(UnauthorizedScheduleOperationException.class,
                            ex -> assertThat(ex.getCode()).isEqualTo(ScheduleErrorCode.UNAUTHORIZED_OPERATION));
            assertThatThrownBy(() -> service.closeSchedule(new ScheduleStatusChangeRequestDTO(s.scheduleId(), 1, true,
                    "x", "admin-1", "SYSTEM_ADMINISTRATOR"))).isInstanceOf(UnauthorizedScheduleOperationException.class);
            assertThat(auditActions()).contains("SCHEDULE_ACCESS_DENIED");
        }

        @Test
        @DisplayName("Patients cannot even consult schedules")
        void patientDenied() {
            assertThatThrownBy(() -> service.searchSchedules(null, "pat-1", "PATIENT"))
                    .isInstanceOf(UnauthorizedScheduleOperationException.class);
        }
    }

    @Nested
    @DisplayName("Booking hand-off (UC-AAD-05)")
    class Booking {

        @Test
        @DisplayName("Reserve is atomic: stale version, full slot and unknown slot are rejected")
        void reserve() {
            ScheduleDTO s = openDefault();
            SlotDTO slot = s.slots().get(0);

            SlotDTO reserved = service.reserveSlotAtomically(slot.slotId(), slot.version(), "BKG-1");
            assertThat(reserved.bookedCount()).isEqualTo(1);
            assertThat(reserved.bookable()).isFalse();
            assertThat(service.reserveSlotAtomically(slot.slotId(), slot.version(), "BKG-1").bookedCount())
                    .as("idempotent for the same booking").isEqualTo(1);
            assertThatThrownBy(() -> service.reserveSlotAtomically(slot.slotId(), slot.version(), "BKG-2"))
                    .isInstanceOf(ConcurrentScheduleChangeException.class);
            assertThatThrownBy(() -> service.reserveSlotAtomically(slot.slotId(), reserved.version(), "BKG-2"))
                    .isInstanceOf(SlotUnavailableException.class).hasMessageContaining("full");
            assertThatThrownBy(() -> service.reserveSlotAtomically("SCH-NOPE@20261201T0900", 1, "BKG-3"))
                    .isInstanceOf(ScheduleNotFoundException.class);
            assertThatThrownBy(() -> service.reserveSlotAtomically("garbage", 1, "BKG-3"))
                    .isInstanceOf(ScheduleNotFoundException.class);
            assertThatThrownBy(() -> service.reserveSlotAtomically(s.scheduleId() + "@20261201T0915", 1, "BKG-3"))
                    .isInstanceOf(ScheduleNotFoundException.class);
            assertThatThrownBy(() -> service.reserveSlotAtomically(slot.slotId(), 1, " "))
                    .isInstanceOf(SlotUnavailableException.class);
            assertThat(auditActions()).contains("SLOT_RESERVED");
        }

        @Test
        @DisplayName("Release gives the place back and is idempotent")
        void release() {
            ScheduleDTO s = openDefault();
            SlotDTO slot = s.slots().get(0);
            service.reserveSlotAtomically(slot.slotId(), slot.version(), "BKG-1");

            service.releaseSlot(slot.slotId(), "BKG-1");
            service.releaseSlot(slot.slotId(), "BKG-1");

            assertThat(service.getSchedule(s.scheduleId(), STAFF, STAFF_ROLE).slots().get(0).bookedCount()).isZero();
            assertThat(auditActions()).filteredOn("SLOT_RELEASED"::equals).hasSize(1);
        }

        @Test
        @DisplayName("Audit failures do not undo the change")
        void auditFailureIgnored() {
            doThrow(new IllegalStateException("audit down")).when(auditService).recordEvent(any());
            assertThat(openDefault().status()).isEqualTo(ScheduleStatus.OPEN);
        }
    }
}
