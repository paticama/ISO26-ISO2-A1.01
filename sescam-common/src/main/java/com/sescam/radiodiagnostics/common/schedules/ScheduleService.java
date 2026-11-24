package com.sescam.radiodiagnostics.common.schedules;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public contract of C3 (sescam-schedules), UC-AAD-02: Manage Schedules.
 *
 * <p>The first group of methods is used by Admissions Staff through the REST API. The second group is the
 * "Schedule interface" consumed in-process by C5 (sescam-admissions) for UC-AAD-05: those methods take no
 * actor because the caller has already checked the role of its own user.
 */
public interface ScheduleService {

    // --- UC-AAD-02: Admissions Staff ---------------------------------------------------------------------

    /** Steps 1-2: schedules matching the date, resource, location or status, ordered by start. */
    List<ScheduleSummaryDTO> searchSchedules(ScheduleSearchCriteriaDTO criteria, String actorId, String actorRole);

    /** Step 2: one schedule with its slots, capacity and bookings. */
    ScheduleDTO getSchedule(String scheduleId, String actorId, String actorRole);

    /** Open a new schedule (A1, A5, A6). */
    ScheduleChangeResultDTO openSchedule(OpenScheduleRequestDTO request);

    /** Update time window, slot duration or capacity (A1, A2, A5, A6). */
    ScheduleChangeResultDTO updateSchedule(UpdateScheduleRequestDTO request);

    /** Close: no new bookings, existing appointments preserved (A2, A3). */
    ScheduleChangeResultDTO closeSchedule(ScheduleStatusChangeRequestDTO request);

    /** Reopen: capacity and conflicts revalidated before slots become bookable (A4). */
    ScheduleChangeResultDTO reopenSchedule(ScheduleStatusChangeRequestDTO request);

    // --- Schedule interface for C5 (UC-AAD-05) -------------------------------------------------------------

    /**
     * Postcondition 3: bookable slots (open schedule, future, free capacity) of a modality whose start is in
     * {@code [from, to)}, ordered by start. A null modality returns every modality.
     */
    List<SlotDTO> findCompatibleSlots(String modality, LocalDateTime from, LocalDateTime to);

    /**
     * Takes one place of the slot for a booking, atomically.
     *
     * @throws SlotUnavailableException           schedule closed, slot full or in the past
     * @throws ConcurrentScheduleChangeException  {@code expectedVersion} is stale
     * @throws ScheduleNotFoundException          unknown slot
     */
    SlotDTO reserveSlotAtomically(String slotId, long expectedVersion, String bookingReference);

    /**
     * Gives back the place held by a booking (cancellation or rescheduling), also when the booking was in
     * the pending-rescheduling list of the schedule. Idempotent.
     *
     * @throws ScheduleNotFoundException unknown schedule
     */
    void releaseSlot(String slotId, String bookingReference);
}
