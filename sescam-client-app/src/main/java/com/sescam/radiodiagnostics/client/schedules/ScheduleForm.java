package com.sescam.radiodiagnostics.client.schedules;

import com.sescam.radiodiagnostics.common.schedules.OpenScheduleRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatusChangeRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.UpdateScheduleRequestDTO;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Form of the schedule view (step 3). Validates the fields before calling the server, so obvious mistakes
 * (A5) are shown without a round trip. The server validates again.
 */
public class ScheduleForm {

    static final int MIN_SLOT_MINUTES = 5;

    private String scheduleId;
    private String resourceId;
    private String locationId;
    private String modality;
    private String date;
    private String startTime;
    private String endTime;
    private Integer slotDurationMinutes = 30;
    private Integer capacityPerSlot = 1;
    private long expectedVersion;
    private boolean confirmAffectedAppointments;
    private String reason;

    public static ScheduleForm fromDTO(ScheduleDTO s) {
        ScheduleForm f = new ScheduleForm();
        f.scheduleId = s.scheduleId();
        f.resourceId = s.resourceId();
        f.locationId = s.locationId();
        f.modality = s.modality();
        f.date = s.start().toLocalDate().toString();
        f.startTime = s.start().toLocalTime().toString();
        f.endTime = s.end().toLocalTime().toString();
        f.slotDurationMinutes = s.slotDurationMinutes();
        f.capacityPerSlot = s.capacityPerSlot();
        f.expectedVersion = s.version();
        return f;
    }

    /** Fields of a new schedule. */
    public Map<String, String> validateOpen() {
        Map<String, String> errors = new LinkedHashMap<>();
        if (isBlank(resourceId)) {
            errors.put("resourceId", "Resource (room or equipment) is mandatory.");
        }
        if (isBlank(locationId)) {
            errors.put("locationId", "Location is mandatory.");
        }
        if (isBlank(modality)) {
            errors.put("modality", "Modality is mandatory.");
        }
        errors.putAll(validateWindow());
        return errors;
    }

    /** Fields that an update can change. */
    public Map<String, String> validateWindow() {
        Map<String, String> errors = new LinkedHashMap<>();
        LocalDate day = parseDate(errors);
        LocalTime from = parseTime(startTime, "startTime", "Start time", errors);
        LocalTime to = parseTime(endTime, "endTime", "End time", errors);
        if (day != null && from != null && to != null && !to.isAfter(from)) {
            errors.put("endTime", "End time must be after the start time.");
        }
        if (slotDurationMinutes == null || slotDurationMinutes < MIN_SLOT_MINUTES) {
            errors.put("slotDurationMinutes", "Slot duration must be at least " + MIN_SLOT_MINUTES + " minutes.");
        } else if (from != null && to != null && to.isAfter(from)
                && Duration.between(from, to).toMinutes() % slotDurationMinutes != 0) {
            errors.put("slotDurationMinutes", "The time window must be a multiple of the slot duration.");
        }
        if (capacityPerSlot == null || capacityPerSlot < 1) {
            errors.put("capacityPerSlot", "Capacity per slot must be at least 1.");
        }
        return errors;
    }

    /** Close needs a reason. */
    public Map<String, String> validateClose() {
        Map<String, String> errors = new LinkedHashMap<>();
        if (isBlank(reason)) {
            errors.put("reason", "A reason is mandatory to close a schedule.");
        }
        return errors;
    }

    public OpenScheduleRequestDTO toOpenRequest(String actorId, String actorRole) {
        return new OpenScheduleRequestDTO(resourceId.trim(), locationId.trim(), modality.trim(), start(), end(),
                slotDurationMinutes, capacityPerSlot, actorId, actorRole);
    }

    public UpdateScheduleRequestDTO toUpdateRequest(String actorId, String actorRole) {
        return new UpdateScheduleRequestDTO(scheduleId, start(), end(), slotDurationMinutes, capacityPerSlot,
                expectedVersion, confirmAffectedAppointments, blankToNull(reason), actorId, actorRole);
    }

    public ScheduleStatusChangeRequestDTO toStatusChangeRequest(String actorId, String actorRole) {
        return new ScheduleStatusChangeRequestDTO(scheduleId, expectedVersion, confirmAffectedAppointments,
                blankToNull(reason), actorId, actorRole);
    }

    LocalDateTime start() {
        return LocalDate.parse(date.trim()).atTime(LocalTime.parse(startTime.trim()));
    }

    LocalDateTime end() {
        LocalTime to = LocalTime.parse(endTime.trim());
        LocalDate day = LocalDate.parse(date.trim());
        return to.equals(LocalTime.MIDNIGHT) ? day.plusDays(1).atStartOfDay() : day.atTime(to);
    }

    private LocalDate parseDate(Map<String, String> errors) {
        if (isBlank(date)) {
            errors.put("date", "Date is mandatory.");
            return null;
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException ex) {
            errors.put("date", "Date must have the format yyyy-MM-dd.");
            return null;
        }
    }

    private static LocalTime parseTime(String value, String field, String label, Map<String, String> errors) {
        if (isBlank(value)) {
            errors.put(field, label + " is mandatory.");
            return null;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            errors.put(field, label + " must have the format HH:mm.");
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    // --- Getters and setters for form binding ---------------------------------------------------------------

    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }
    public String getModality() { return modality; }
    public void setModality(String modality) { this.modality = modality; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public Integer getSlotDurationMinutes() { return slotDurationMinutes; }
    public void setSlotDurationMinutes(Integer slotDurationMinutes) { this.slotDurationMinutes = slotDurationMinutes; }
    public Integer getCapacityPerSlot() { return capacityPerSlot; }
    public void setCapacityPerSlot(Integer capacityPerSlot) { this.capacityPerSlot = capacityPerSlot; }
    public long getExpectedVersion() { return expectedVersion; }
    public void setExpectedVersion(long expectedVersion) { this.expectedVersion = expectedVersion; }
    public boolean isConfirmAffectedAppointments() { return confirmAffectedAppointments; }
    public void setConfirmAffectedAppointments(boolean confirm) { this.confirmAffectedAppointments = confirm; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
