package com.sescam.radiodiagnostics.server.schedules;

import com.sescam.radiodiagnostics.common.schedules.OpenScheduleRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleChangeResultDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleService;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatus;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatusChangeRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleSummaryDTO;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import com.sescam.radiodiagnostics.common.schedules.UpdateScheduleRequestDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST API for UC-AAD-02: Manage Schedules. The actor comes in the {@code X-Actor-Id} and
 * {@code X-Actor-Role} headers, as in the other use cases.
 */
@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleRestController {

    private final ScheduleService scheduleService;

    public ScheduleRestController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /** Steps 1-2: schedules of a date, resource or location. */
    @GetMapping
    public List<ScheduleSummaryDTO> search(
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "resourceId", required = false) String resourceId,
            @RequestParam(value = "locationId", required = false) String locationId,
            @RequestParam(value = "status", required = false) ScheduleStatus status) {
        return scheduleService.searchSchedules(new ScheduleSearchCriteriaDTO(date, resourceId, locationId, status),
                actorId, actorRole);
    }

    /** Step 2: one schedule with its slots and bookings. */
    @GetMapping("/{id}")
    public ScheduleDTO get(@PathVariable("id") String id,
                           @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                           @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return scheduleService.getSchedule(id, actorId, actorRole);
    }

    /** Open (201). */
    @PostMapping
    public ResponseEntity<ScheduleChangeResultDTO> open(
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestBody OpenSchedulePayload payload) {
        ScheduleChangeResultDTO result = scheduleService.openSchedule(new OpenScheduleRequestDTO(payload.resourceId(),
                payload.locationId(), payload.modality(), payload.start(), payload.end(),
                payload.slotDurationMinutes(), payload.capacityPerSlot(), actorId, actorRole));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /** Update. 409 APPOINTMENTS_AFFECTED lists the appointments until {@code confirmAffectedAppointments}. */
    @PutMapping("/{id}")
    public ScheduleChangeResultDTO update(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestBody UpdateSchedulePayload payload) {
        return scheduleService.updateSchedule(new UpdateScheduleRequestDTO(id, payload.start(), payload.end(),
                payload.slotDurationMinutes(), payload.capacityPerSlot(), payload.expectedVersion(),
                payload.confirmAffectedAppointments(), payload.reason(), actorId, actorRole));
    }

    /** Close (A3). */
    @PostMapping("/{id}/close")
    public ScheduleChangeResultDTO close(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestBody StatusChangePayload payload) {
        return scheduleService.closeSchedule(new ScheduleStatusChangeRequestDTO(id, payload.expectedVersion(),
                payload.confirmAffectedAppointments(), payload.reason(), actorId, actorRole));
    }

    /** Reopen (A4). */
    @PostMapping("/{id}/reopen")
    public ScheduleChangeResultDTO reopen(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestBody StatusChangePayload payload) {
        return scheduleService.reopenSchedule(new ScheduleStatusChangeRequestDTO(id, payload.expectedVersion(),
                false, payload.reason(), actorId, actorRole));
    }

    /**
     * Postcondition 3: bookable slots exposed to the booking workflow (read only; reservation is done
     * in-process by C5 through {@link ScheduleService#reserveSlotAtomically}).
     */
    @GetMapping("/slots/available")
    public List<SlotDTO> availableSlots(
            @RequestParam(value = "modality", required = false) String modality,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return scheduleService.findCompatibleSlots(modality, from, to);
    }

    public record OpenSchedulePayload(String resourceId, String locationId, String modality, LocalDateTime start,
                                      LocalDateTime end, int slotDurationMinutes, int capacityPerSlot) {
    }

    public record UpdateSchedulePayload(LocalDateTime start, LocalDateTime end, int slotDurationMinutes,
                                        int capacityPerSlot, long expectedVersion,
                                        boolean confirmAffectedAppointments, String reason) {
    }

    public record StatusChangePayload(long expectedVersion, boolean confirmAffectedAppointments, String reason) {
    }
}
