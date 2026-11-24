package com.sescam.radiodiagnostics.schedules.domain;

import com.sescam.radiodiagnostics.common.schedules.AffectedAppointmentDTO;
import com.sescam.radiodiagnostics.common.schedules.ConcurrentScheduleChangeException;
import com.sescam.radiodiagnostics.common.schedules.ScheduleDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleNotFoundException;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatus;
import com.sescam.radiodiagnostics.common.schedules.ScheduleSummaryDTO;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import com.sescam.radiodiagnostics.common.schedules.SlotUnavailableException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Entity of C3: examination schedule of one resource (room or equipment) at one location for one modality.
 * The time window is divided into slots of {@code slotDurationMinutes}; each slot has
 * {@code capacityPerSlot} places that C5 reserves for its bookings.
 *
 * <p>Changes never drop an appointment silently: appointments that no longer fit are moved to
 * {@link #getAppointmentsPendingRescheduling()} and handed to admissions (postcondition 2).
 */
public class Schedule {

    /** Separates the schedule id from the slot start in slot ids, e.g. {@code SCH-1A2B3C4D@20261201T0900}. */
    public static final char SLOT_ID_SEPARATOR = '@';
    private static final DateTimeFormatter SLOT_ID_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm");

    private final String id;
    private final String resourceId;
    private final String locationId;
    private final String modality;
    private final Instant createdAt;
    private LocalDateTime start;
    private LocalDateTime end;
    private int slotDurationMinutes;
    private int capacityPerSlot;
    private ScheduleStatus status;
    private long version;
    private List<Slot> slots;
    private final List<AffectedAppointmentDTO> appointmentsPendingRescheduling = new ArrayList<>();
    private String lastChangeReason;
    private Instant updatedAt;

    public Schedule(String id, String resourceId, String locationId, String modality, LocalDateTime start,
                    LocalDateTime end, int slotDurationMinutes, int capacityPerSlot, Instant now) {
        this.id = id;
        this.resourceId = resourceId;
        this.locationId = locationId;
        this.modality = modality;
        this.start = start;
        this.end = end;
        this.slotDurationMinutes = slotDurationMinutes;
        this.capacityPerSlot = capacityPerSlot;
        this.status = ScheduleStatus.OPEN;
        this.version = 1;
        this.createdAt = now;
        this.updatedAt = now;
        this.slots = buildSlots(start, end, slotDurationMinutes, capacityPerSlot, Map.of(), Map.of());
    }

    /** Slot id for a schedule and a slot start. */
    public static String slotId(String scheduleId, LocalDateTime slotStart) {
        return scheduleId + SLOT_ID_SEPARATOR + SLOT_ID_FORMAT.format(slotStart);
    }

    /** Schedule id contained in a slot id, if the slot id is well formed. */
    public static Optional<String> scheduleIdOf(String slotId) {
        if (slotId == null) {
            return Optional.empty();
        }
        int at = slotId.indexOf(SLOT_ID_SEPARATOR);
        return at <= 0 ? Optional.empty() : Optional.of(slotId.substring(0, at));
    }

    // --- Queries -------------------------------------------------------------------------------------------

    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return start.isBefore(otherEnd) && otherStart.isBefore(end);
    }

    public boolean isOpen() {
        return status == ScheduleStatus.OPEN;
    }

    public int getBookedCount() {
        return slots.stream().mapToInt(Slot::getBookedCount).sum();
    }

    public Optional<Slot> findSlot(String slotId) {
        return slots.stream().filter(s -> s.getId().equals(slotId)).findFirst();
    }

    /**
     * A2: the appointments that would lose their place if the window, slot duration or capacity changed.
     * An appointment keeps its place only if a slot with the same start and end still exists and has room
     * for it (earlier reservations keep their place first).
     */
    public List<AffectedAppointmentDTO> appointmentsAffectedBy(LocalDateTime newStart, LocalDateTime newEnd,
                                                               int newSlotMinutes, int newCapacity) {
        List<AffectedAppointmentDTO> affected = new ArrayList<>();
        for (Slot slot : slots) {
            boolean slotKept = fitsGrid(slot, newStart, newEnd, newSlotMinutes);
            List<String> refs = slot.getBookingReferences();
            for (int i = 0; i < refs.size(); i++) {
                if (!slotKept) {
                    affected.add(affected(slot, refs.get(i), "Slot removed by the new time window or slot duration"));
                } else if (i >= newCapacity) {
                    affected.add(affected(slot, refs.get(i), "Slot capacity reduced to " + newCapacity));
                }
            }
        }
        return affected;
    }

    /** A3: every appointment of the schedule; on close they are preserved but listed for admissions. */
    public List<AffectedAppointmentDTO> appointmentsAffectedByClosing() {
        List<AffectedAppointmentDTO> affected = new ArrayList<>();
        for (Slot slot : slots) {
            for (String ref : slot.getBookingReferences()) {
                affected.add(affected(slot, ref, "Schedule closed: appointment preserved, admissions decides"));
            }
        }
        return affected;
    }

    // --- Commands ------------------------------------------------------------------------------------------

    public void checkVersion(long expectedVersion) {
        if (version != expectedVersion) {
            throw new ConcurrentScheduleChangeException("Schedule '" + id + "' was changed by someone else. Expected"
                    + " version " + expectedVersion + ", current version " + version + ". Reload and retry.");
        }
    }

    /**
     * Applies a confirmed update. Appointments that still fit keep their slot; the others are returned and
     * kept in the pending-rescheduling list.
     */
    public List<AffectedAppointmentDTO> update(LocalDateTime newStart, LocalDateTime newEnd, int newSlotMinutes,
                                               int newCapacity, String reason, Instant now) {
        List<AffectedAppointmentDTO> affected = appointmentsAffectedBy(newStart, newEnd, newSlotMinutes, newCapacity);
        Map<LocalDateTime, List<String>> keptRefs = new LinkedHashMap<>();
        Map<LocalDateTime, Long> keptVersions = new LinkedHashMap<>();
        for (Slot slot : slots) {
            if (fitsGrid(slot, newStart, newEnd, newSlotMinutes)) {
                List<String> refs = slot.getBookingReferences();
                keptRefs.put(slot.getStart(), new ArrayList<>(refs.subList(0, Math.min(refs.size(), newCapacity))));
                keptVersions.put(slot.getStart(), slot.getVersion() + 1);
            }
        }
        this.start = newStart;
        this.end = newEnd;
        this.slotDurationMinutes = newSlotMinutes;
        this.capacityPerSlot = newCapacity;
        this.slots = buildSlots(newStart, newEnd, newSlotMinutes, newCapacity, keptRefs, keptVersions);
        appointmentsPendingRescheduling.addAll(affected);
        touch(reason, now);
        return affected;
    }

    /** A3: closes the schedule. Appointments stay in their slots; no new bookings are accepted. */
    public void close(String reason, Instant now) {
        this.status = ScheduleStatus.CLOSED;
        touch(reason, now);
    }

    /** A4: reopens the schedule. Validation of capacity and conflicts is done by the service before. */
    public void reopen(String reason, Instant now) {
        this.status = ScheduleStatus.OPEN;
        touch(reason, now);
    }

    /** Takes a place of the slot for a booking (UC-AAD-05). Idempotent for the same booking reference. */
    public Slot reserve(String slotId, long expectedSlotVersion, String bookingReference, LocalDateTime now) {
        Slot slot = findSlot(slotId)
                .orElseThrow(() -> new ScheduleNotFoundException("Slot '" + slotId + "' does not exist."));
        if (slot.holds(bookingReference)) {
            return slot;
        }
        if (!isOpen()) {
            throw new SlotUnavailableException("Schedule '" + id + "' is closed and accepts no new bookings.");
        }
        if (!slot.getStart().isAfter(now)) {
            throw new SlotUnavailableException("Slot '" + slotId + "' has already started.");
        }
        if (slot.getVersion() != expectedSlotVersion) {
            throw new ConcurrentScheduleChangeException("Slot '" + slotId + "' changed (expected version "
                    + expectedSlotVersion + ", current " + slot.getVersion() + ").");
        }
        if (!slot.hasFreeCapacity()) {
            throw new SlotUnavailableException("Slot '" + slotId + "' is full.");
        }
        slot.addBooking(bookingReference);
        return slot;
    }

    /**
     * Gives back the place of a booking and removes it from the pending-rescheduling list. Idempotent.
     *
     * @return true if the booking was found in the slot or in the pending-rescheduling list
     */
    public boolean release(String slotId, String bookingReference) {
        boolean pending = appointmentsPendingRescheduling.removeIf(
                a -> a.slotId().equals(slotId) && a.bookingReference().equals(bookingReference));
        boolean inSlot = findSlot(slotId).map(slot -> slot.removeBooking(bookingReference)).orElse(false);
        return pending || inSlot;
    }

    // --- Mapping -------------------------------------------------------------------------------------------

    public SlotDTO toSlotDTO(Slot slot, LocalDateTime now) {
        boolean bookable = isOpen() && slot.getStart().isAfter(now) && slot.hasFreeCapacity();
        return new SlotDTO(slot.getId(), id, resourceId, locationId, modality, slot.getStart(), slot.getEnd(),
                slot.getCapacity(), slot.getBookedCount(), slot.getVersion(), bookable);
    }

    public ScheduleDTO toDTO(LocalDateTime now) {
        return new ScheduleDTO(id, resourceId, locationId, modality, start, end, slotDurationMinutes,
                capacityPerSlot, status, version, slots.stream().map(s -> toSlotDTO(s, now)).toList(),
                appointmentsPendingRescheduling, lastChangeReason, updatedAt);
    }

    public ScheduleSummaryDTO toSummaryDTO() {
        return new ScheduleSummaryDTO(id, resourceId, locationId, modality, start, end, status,
                slots.size() * capacityPerSlot, getBookedCount());
    }

    // --- Internals -----------------------------------------------------------------------------------------

    private boolean fitsGrid(Slot slot, LocalDateTime newStart, LocalDateTime newEnd, int newSlotMinutes) {
        if (slot.getStart().isBefore(newStart) || slot.getEnd().isAfter(newEnd)) {
            return false;
        }
        long offset = java.time.Duration.between(newStart, slot.getStart()).toMinutes();
        long length = java.time.Duration.between(slot.getStart(), slot.getEnd()).toMinutes();
        return length == newSlotMinutes && offset % newSlotMinutes == 0;
    }

    private List<Slot> buildSlots(LocalDateTime from, LocalDateTime to, int minutes, int capacity,
                                  Map<LocalDateTime, List<String>> refs, Map<LocalDateTime, Long> versions) {
        List<Slot> result = new ArrayList<>();
        for (LocalDateTime s = from; s.isBefore(to); s = s.plusMinutes(minutes)) {
            result.add(new Slot(slotId(id, s), s, s.plusMinutes(minutes), capacity,
                    refs.getOrDefault(s, List.of()), versions.getOrDefault(s, 1L)));
        }
        return result;
    }

    private AffectedAppointmentDTO affected(Slot slot, String ref, String reason) {
        return new AffectedAppointmentDTO(ref, id, slot.getId(), slot.getStart(), slot.getEnd(), reason);
    }

    private void touch(String reason, Instant now) {
        this.version++;
        this.lastChangeReason = reason;
        this.updatedAt = now;
    }

    // --- Getters -------------------------------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getLocationId() {
        return locationId;
    }

    public String getModality() {
        return modality;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public int getSlotDurationMinutes() {
        return slotDurationMinutes;
    }

    public int getCapacityPerSlot() {
        return capacityPerSlot;
    }

    public ScheduleStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public List<Slot> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    public List<AffectedAppointmentDTO> getAppointmentsPendingRescheduling() {
        return Collections.unmodifiableList(appointmentsPendingRescheduling);
    }

    public String getLastChangeReason() {
        return lastChangeReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
