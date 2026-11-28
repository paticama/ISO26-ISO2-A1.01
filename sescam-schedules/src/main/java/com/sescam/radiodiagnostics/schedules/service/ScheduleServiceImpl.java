package com.sescam.radiodiagnostics.schedules.service;

import com.sescam.radiodiagnostics.common.schedules.AffectedAppointmentDTO;
import com.sescam.radiodiagnostics.common.schedules.AppointmentsAffectedException;
import com.sescam.radiodiagnostics.common.schedules.InvalidScheduleException;
import com.sescam.radiodiagnostics.common.schedules.OpenScheduleRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleChangeResultDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleChangedEvent;
import com.sescam.radiodiagnostics.common.schedules.ScheduleDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleEventPublisher;
import com.sescam.radiodiagnostics.common.schedules.ScheduleException;
import com.sescam.radiodiagnostics.common.schedules.ScheduleNotFoundException;
import com.sescam.radiodiagnostics.common.schedules.ScheduleOperation;
import com.sescam.radiodiagnostics.common.schedules.ScheduleOverlapException;
import com.sescam.radiodiagnostics.common.schedules.ScheduleSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleService;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatus;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatusChangeRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleSummaryDTO;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import com.sescam.radiodiagnostics.common.schedules.SlotUnavailableException;
import com.sescam.radiodiagnostics.common.schedules.UnauthorizedScheduleOperationException;
import com.sescam.radiodiagnostics.common.schedules.UpdateScheduleRequestDTO;
import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.schedules.domain.Schedule;
import com.sescam.radiodiagnostics.schedules.domain.Slot;
import com.sescam.radiodiagnostics.schedules.internal.ScheduleAccessPolicy;
import com.sescam.radiodiagnostics.schedules.internal.ScheduleRules;
import com.sescam.radiodiagnostics.schedules.repository.ScheduleRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Realization of UC-AAD-02: Manage Schedules (C3 sescam-schedules v1.0.0).
 *
 * <p>Every change runs the same steps: role check (A6), validation of dates and capacity (A5), overlap
 * check (A1), affected-appointment check (A2), persist, audit entry and event to admissions. Changing
 * methods are {@code synchronized} so validation and persistence are atomic with the in-memory repository.
 */
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository repository;
    private final ScheduleRules rules;
    private final ScheduleAccessPolicy accessPolicy;
    private final AuditService auditService;
    private final ScheduleEventPublisher eventPublisher;
    private final Clock clock;

    public ScheduleServiceImpl(ScheduleRepository repository, ScheduleRules rules, ScheduleAccessPolicy accessPolicy,
                               AuditService auditService, ScheduleEventPublisher eventPublisher, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.rules = Objects.requireNonNull(rules, "rules must not be null");
        this.accessPolicy = Objects.requireNonNull(accessPolicy, "accessPolicy must not be null");
        this.auditService = Objects.requireNonNull(auditService, "auditService must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // --- Consult -------------------------------------------------------------------------------------------

    @Override
    public List<ScheduleSummaryDTO> searchSchedules(ScheduleSearchCriteriaDTO criteria, String actorId,
                                                    String actorRole) {
        checkView(actorId, actorRole, "SEARCH_SCHEDULES");
        return repository.search(criteria).stream().map(Schedule::toSummaryDTO).toList();
    }

    @Override
    public ScheduleDTO getSchedule(String scheduleId, String actorId, String actorRole) {
        checkView(actorId, actorRole, "GET_SCHEDULE");
        return load(scheduleId).toDTO(now());
    }

    // --- Change --------------------------------------------------------------------------------------------

    @Override
    public synchronized ScheduleChangeResultDTO openSchedule(OpenScheduleRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");
        checkOperate(request.actorId(), request.actorRole(), ScheduleOperation.OPEN, "new schedule");
        String resourceId = trim(request.resourceId());
        String locationId = trim(request.locationId());
        String modality = request.modality() == null ? null : request.modality().trim().toUpperCase(Locale.ROOT);
        try {
            rules.validateIdentity(resourceId, locationId, modality);
            rules.validateWindow(request.start(), request.end(), request.slotDurationMinutes(),
                    request.capacityPerSlot(), now());
            checkOverlap(null, resourceId, request.start(), request.end());
        } catch (ScheduleException ex) {
            auditRejected(request.actorId(), ScheduleOperation.OPEN, resourceId, ex);
            throw ex;
        }
        String id = "SCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        Schedule schedule = new Schedule(id, resourceId, locationId, modality, request.start(), request.end(),
                request.slotDurationMinutes(), request.capacityPerSlot(), clock.instant());
        repository.save(schedule);
        return committed(ScheduleOperation.OPEN, schedule, List.of(), request.actorId(), null,
                "Opened " + describe(schedule));
    }

    @Override
    public synchronized ScheduleChangeResultDTO updateSchedule(UpdateScheduleRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");
        checkOperate(request.actorId(), request.actorRole(), ScheduleOperation.UPDATE, request.scheduleId());
        Schedule schedule = load(request.scheduleId());
        String before = describe(schedule);
        List<AffectedAppointmentDTO> affected;
        try {
            schedule.checkVersion(request.expectedVersion());
            if (!schedule.isOpen()) {
                throw new InvalidScheduleException("Schedule '" + schedule.getId()
                        + "' is closed. Reopen it before changing it.");
            }
            rules.validateWindow(request.start(), request.end(), request.slotDurationMinutes(),
                    request.capacityPerSlot(), now());
            checkOverlap(schedule.getId(), schedule.getResourceId(), request.start(), request.end());
            affected = schedule.appointmentsAffectedBy(request.start(), request.end(),
                    request.slotDurationMinutes(), request.capacityPerSlot());
            requireConfirmation(affected, request.confirmAffectedAppointments(), schedule.getId());
        } catch (ScheduleException ex) {
            auditRejected(request.actorId(), ScheduleOperation.UPDATE, schedule.getId(), ex);
            throw ex;
        }
        schedule.update(request.start(), request.end(), request.slotDurationMinutes(), request.capacityPerSlot(),
                request.reason(), clock.instant());
        repository.save(schedule);
        return committed(ScheduleOperation.UPDATE, schedule, affected, request.actorId(), before,
                describe(schedule));
    }

    @Override
    public synchronized ScheduleChangeResultDTO closeSchedule(ScheduleStatusChangeRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");
        checkOperate(request.actorId(), request.actorRole(), ScheduleOperation.CLOSE, request.scheduleId());
        Schedule schedule = load(request.scheduleId());
        List<AffectedAppointmentDTO> affected;
        try {
            schedule.checkVersion(request.expectedVersion());
            if (!schedule.isOpen()) {
                throw new InvalidScheduleException("Schedule '" + schedule.getId() + "' is already closed.");
            }
            if (request.reason() == null || request.reason().isBlank()) {
                throw new InvalidScheduleException("A reason is mandatory to close a schedule.");
            }
            affected = schedule.appointmentsAffectedByClosing();
            requireConfirmation(affected, request.confirmAffectedAppointments(), schedule.getId());
        } catch (ScheduleException ex) {
            auditRejected(request.actorId(), ScheduleOperation.CLOSE, schedule.getId(), ex);
            throw ex;
        }
        schedule.close(request.reason().trim(), clock.instant());
        repository.save(schedule);
        return committed(ScheduleOperation.CLOSE, schedule, affected, request.actorId(), ScheduleStatus.OPEN.name(),
                ScheduleStatus.CLOSED.name());
    }

    @Override
    public synchronized ScheduleChangeResultDTO reopenSchedule(ScheduleStatusChangeRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");
        checkOperate(request.actorId(), request.actorRole(), ScheduleOperation.REOPEN, request.scheduleId());
        Schedule schedule = load(request.scheduleId());
        try {
            schedule.checkVersion(request.expectedVersion());
            if (schedule.isOpen()) {
                throw new InvalidScheduleException("Schedule '" + schedule.getId() + "' is already open.");
            }
            // A4: revalidate as if it were new: still in the future, capacity within the current limit and no
            // other schedule opened on the same resource while this one was closed.
            if (!schedule.getEnd().isAfter(now())) {
                throw new InvalidScheduleException("Schedule '" + schedule.getId()
                        + "' has already ended and cannot be reopened.");
            }
            rules.validateCapacity(schedule.getCapacityPerSlot());
            checkOverlap(schedule.getId(), schedule.getResourceId(), schedule.getStart(), schedule.getEnd());
        } catch (ScheduleException ex) {
            auditRejected(request.actorId(), ScheduleOperation.REOPEN, schedule.getId(), ex);
            throw ex;
        }
        schedule.reopen(request.reason(), clock.instant());
        repository.save(schedule);
        return committed(ScheduleOperation.REOPEN, schedule, List.of(), request.actorId(),
                ScheduleStatus.CLOSED.name(), ScheduleStatus.OPEN.name());
    }

    // --- Schedule interface for C5 (UC-AAD-05) -------------------------------------------------------------

    @Override
    public List<SlotDTO> findCompatibleSlots(String modality, LocalDateTime from, LocalDateTime to) {
        LocalDateTime now = now();
        String wanted = modality == null || modality.isBlank() ? null : modality.trim().toUpperCase(Locale.ROOT);
        return repository.search(new ScheduleSearchCriteriaDTO(null, null, null, ScheduleStatus.OPEN)).stream()
                .filter(s -> wanted == null || s.getModality().equals(wanted))
                .flatMap(s -> s.getSlots().stream().map(slot -> s.toSlotDTO(slot, now)))
                .filter(SlotDTO::bookable)
                .filter(slot -> from == null || !slot.start().isBefore(from))
                .filter(slot -> to == null || slot.start().isBefore(to))
                .sorted(Comparator.comparing(SlotDTO::start).thenComparing(SlotDTO::resourceId))
                .toList();
    }

    @Override
    public synchronized SlotDTO reserveSlotAtomically(String slotId, long expectedVersion, String bookingReference) {
        if (bookingReference == null || bookingReference.isBlank()) {
            throw new SlotUnavailableException("A booking reference is mandatory to reserve a slot.");
        }
        Schedule schedule = scheduleOfSlot(slotId);
        Slot slot = schedule.reserve(slotId, expectedVersion, bookingReference, now());
        repository.save(schedule);
        audit("SLOT_RESERVED", "SYSTEM", slotId, null, bookingReference,
                "Booking " + bookingReference + " took a place in slot " + slotId);
        return schedule.toSlotDTO(slot, now());
    }

    @Override
    public synchronized void releaseSlot(String slotId, String bookingReference) {
        Schedule schedule = scheduleOfSlot(slotId);
        if (schedule.release(slotId, bookingReference)) {
            repository.save(schedule);
            audit("SLOT_RELEASED", "SYSTEM", slotId, bookingReference, null,
                    "Booking " + bookingReference + " released slot " + slotId);
        }
    }

    // --- Helpers -------------------------------------------------------------------------------------------

    private void checkView(String actorId, String actorRole, String operation) {
        if (!accessPolicy.canViewSchedules(actorId, actorRole)) {
            denied(actorId, actorRole, operation, "consult schedules");
        }
    }

    private void checkOperate(String actorId, String actorRole, ScheduleOperation operation, String target) {
        if (!accessPolicy.canOperateSchedules(actorId, actorRole)) {
            denied(actorId, actorRole, operation.name() + " " + target, operation.name().toLowerCase(Locale.ROOT)
                    + " schedules (reserved to Admissions Staff)");
        }
    }

    private void denied(String actorId, String actorRole, String operation, String what) {
        audit("SCHEDULE_ACCESS_DENIED", actorId, operation, null, null,
                "User " + actorId + " with role " + actorRole + " attempted " + operation);
        throw new UnauthorizedScheduleOperationException(
                "User '" + actorId + "' with role '" + actorRole + "' is not authorized to " + what + ".");
    }

    private Schedule load(String scheduleId) {
        if (scheduleId == null || scheduleId.isBlank()) {
            throw new InvalidScheduleException("Schedule id must not be blank.");
        }
        return repository.findById(scheduleId.trim())
                .orElseThrow(() -> new ScheduleNotFoundException("Schedule '" + scheduleId + "' not found."));
    }

    private Schedule scheduleOfSlot(String slotId) {
        String scheduleId = Schedule.scheduleIdOf(slotId)
                .orElseThrow(() -> new ScheduleNotFoundException("Slot '" + slotId + "' does not exist."));
        return repository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException("Slot '" + slotId + "' does not exist."));
    }

    /** A1: another open schedule of the same resource overlaps the window. */
    private void checkOverlap(String ownId, String resourceId, LocalDateTime start, LocalDateTime end) {
        List<ScheduleSummaryDTO> conflicts = repository.findByResource(resourceId).stream()
                .filter(Schedule::isOpen)
                .filter(s -> !s.getId().equals(ownId))
                .filter(s -> s.overlaps(start, end))
                .map(Schedule::toSummaryDTO)
                .toList();
        if (!conflicts.isEmpty()) {
            throw new ScheduleOverlapException("Resource '" + resourceId + "' already has " + conflicts.size()
                    + " open schedule(s) between " + start + " and " + end + ".", conflicts);
        }
    }

    /** A2: list the affected appointments and stop unless the change was confirmed. */
    private static void requireConfirmation(List<AffectedAppointmentDTO> affected, boolean confirmed,
                                            String scheduleId) {
        if (!affected.isEmpty() && !confirmed) {
            throw new AppointmentsAffectedException(affected.size() + " appointment(s) of schedule '" + scheduleId
                    + "' are affected. Review them and confirm the change.", affected);
        }
    }

    private ScheduleChangeResultDTO committed(ScheduleOperation operation, Schedule schedule,
                                              List<AffectedAppointmentDTO> affected, String actorId,
                                              String previousValue, String newValue) {
        String details = operation + " " + schedule.getId()
                + (affected.isEmpty() ? "" : "; " + affected.size() + " appointment(s) handed to admissions")
                + (schedule.getLastChangeReason() == null ? "" : "; reason: " + schedule.getLastChangeReason());
        audit("SCHEDULE_" + pastTense(operation), actorId, schedule.getId(), previousValue, newValue, details);
        eventPublisher.publish(new ScheduleChangedEvent(schedule.getId(), operation, schedule.getStatus(), affected,
                actorId, clock.instant()));
        return new ScheduleChangeResultDTO(operation, schedule.toDTO(now()), affected, !affected.isEmpty());
    }

    private void auditRejected(String actorId, ScheduleOperation operation, String target, ScheduleException ex) {
        audit("SCHEDULE_CHANGE_REJECTED", actorId, target, null, null,
                operation + " rejected with " + ex.getCode() + ": " + ex.getMessage());
    }

    private void audit(String action, String actorId, String resourceKey, String previousValue, String newValue,
                       String details) {
        try {
            auditService.recordEvent(new AuditEventDTO(action, actorId == null || actorId.isBlank() ? "SYSTEM" : actorId,
                    resourceKey, previousValue, newValue, details, clock.instant()));
        } catch (RuntimeException ignored) {
            // Same rule as the other modules: an audit failure does not undo the business change.
        }
    }

    private static String pastTense(ScheduleOperation operation) {
        return switch (operation) {
            case OPEN -> "OPENED";
            case UPDATE -> "UPDATED";
            case CLOSE -> "CLOSED";
            case REOPEN -> "REOPENED";
        };
    }

    private static String describe(Schedule s) {
        return s.getResourceId() + "@" + s.getLocationId() + " " + s.getModality() + " " + s.getStart() + "-"
                + s.getEnd().toLocalTime() + " slot " + s.getSlotDurationMinutes() + " min x"
                + s.getCapacityPerSlot();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
