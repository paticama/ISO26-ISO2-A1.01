package com.sescam.radiodiagnostics.schedules.internal;

import com.sescam.radiodiagnostics.common.catalogue.PolicyService;
import com.sescam.radiodiagnostics.common.schedules.CapacityExceededException;
import com.sescam.radiodiagnostics.common.schedules.InvalidScheduleException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Step 5 validations of UC-AAD-02: dates, slot duration and capacity (A5), and the capacity limit configured
 * by the System Administrator in C2 (precondition 2, "scheduling rules are configured").
 */
public class ScheduleRules {

    /** C2 parameter with the maximum places per slot. Admissions Staff cannot go above it. */
    public static final String MAX_CAPACITY_PER_SLOT_KEY = "schedules.max-capacity-per-slot";
    public static final int DEFAULT_MAX_CAPACITY_PER_SLOT = 4;
    /** Shortest slot accepted, in minutes. */
    public static final int MIN_SLOT_MINUTES = 5;

    private final PolicyService policyService;

    public ScheduleRules(PolicyService policyService) {
        this.policyService = Objects.requireNonNull(policyService, "policyService must not be null");
    }

    public int maxCapacityPerSlot() {
        return policyService.getIntPolicy(MAX_CAPACITY_PER_SLOT_KEY, DEFAULT_MAX_CAPACITY_PER_SLOT);
    }

    /** Resource, location and modality of a new schedule. */
    public void validateIdentity(String resourceId, String locationId, String modality) {
        if (isBlank(resourceId)) {
            throw new InvalidScheduleException("Resource (room or equipment) is mandatory.");
        }
        if (isBlank(locationId)) {
            throw new InvalidScheduleException("Location is mandatory.");
        }
        if (isBlank(modality)) {
            throw new InvalidScheduleException("Modality is mandatory.");
        }
    }

    /** Time window, slot duration and capacity. */
    public void validateWindow(LocalDateTime start, LocalDateTime end, int slotMinutes, int capacity,
                               LocalDateTime now) {
        if (start == null || end == null) {
            throw new InvalidScheduleException("Start and end of the schedule are mandatory.");
        }
        if (!end.isAfter(start)) {
            throw new InvalidScheduleException("The end of the schedule must be after its start.");
        }
        if (!start.toLocalDate().equals(end.minusNanos(1).toLocalDate())) {
            throw new InvalidScheduleException("A schedule covers a single day; open one schedule per day.");
        }
        if (start.isBefore(now)) {
            throw new InvalidScheduleException("The schedule cannot start in the past.");
        }
        long windowMinutes = Duration.between(start, end).toMinutes();
        if (slotMinutes < MIN_SLOT_MINUTES) {
            throw new InvalidScheduleException("Slot duration must be at least " + MIN_SLOT_MINUTES + " minutes.");
        }
        if (windowMinutes % slotMinutes != 0) {
            throw new InvalidScheduleException("The time window (" + windowMinutes + " min) must be a multiple of"
                    + " the slot duration (" + slotMinutes + " min).");
        }
        validateCapacity(capacity);
    }

    /** Capacity per slot: at least 1 (A5) and at most the configured limit (CAPACITY_EXCEEDED). */
    public void validateCapacity(int capacity) {
        if (capacity < 1) {
            throw new InvalidScheduleException("Capacity per slot must be at least 1.");
        }
        int max = maxCapacityPerSlot();
        if (capacity > max) {
            throw new CapacityExceededException("Capacity per slot " + capacity + " exceeds the limit of " + max
                    + " configured by the System Administrator (" + MAX_CAPACITY_PER_SLOT_KEY + ").");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
