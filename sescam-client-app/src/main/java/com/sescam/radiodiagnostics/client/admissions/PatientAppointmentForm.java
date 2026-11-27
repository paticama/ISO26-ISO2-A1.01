package com.sescam.radiodiagnostics.client.admissions;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Form validation and payload conversion for patient appointment requests (UC-PAT-01). */
public class PatientAppointmentForm {

    private String reason;
    private String preferredDates;
    private boolean cancellationConfirmed;
    private boolean priorityWarningAcknowledged;

    public Map<String, String> validateReschedule() {
        Map<String, String> errors = new LinkedHashMap<>();
        if (isBlank(reason)) errors.put("reason", "Enter a reason for requesting a change.");
        parsePreferredDates(errors);
        return errors;
    }

    public Map<String, String> validateCancellation(boolean priorityExamination) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!cancellationConfirmed) errors.put("cancellationConfirmed", "Confirm that you want to cancel this appointment.");
        if (priorityExamination && !priorityWarningAcknowledged) {
            errors.put("priorityWarningAcknowledged", "Acknowledge the clinical priority warning before cancelling.");
        }
        return errors;
    }

    public List<LocalDate> parsedPreferredDates() {
        Map<String, String> errors = new LinkedHashMap<>();
        List<LocalDate> dates = parsePreferredDates(errors);
        if (!errors.isEmpty()) throw new IllegalStateException("Preferred dates must be validated before conversion.");
        return dates;
    }

    private List<LocalDate> parsePreferredDates(Map<String, String> errors) {
        if (isBlank(preferredDates)) return List.of();
        List<LocalDate> dates = new ArrayList<>();
        for (String value : preferredDates.split(",")) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) continue;
            try {
                LocalDate date = LocalDate.parse(trimmed);
                if (date.isBefore(LocalDate.now())) {
                    errors.put("preferredDates", "Preferred dates cannot be in the past.");
                    return List.of();
                }
                dates.add(date);
            } catch (DateTimeParseException ex) {
                errors.put("preferredDates", "Enter preferred dates in YYYY-MM-DD format, separated by commas.");
                return List.of();
            }
        }
        return List.copyOf(dates);
    }

    private static boolean isBlank(String value) { return value == null || value.isBlank(); }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getPreferredDates() { return preferredDates; }
    public void setPreferredDates(String preferredDates) { this.preferredDates = preferredDates; }
    public boolean isCancellationConfirmed() { return cancellationConfirmed; }
    public void setCancellationConfirmed(boolean cancellationConfirmed) { this.cancellationConfirmed = cancellationConfirmed; }
    public boolean isPriorityWarningAcknowledged() { return priorityWarningAcknowledged; }
    public void setPriorityWarningAcknowledged(boolean priorityWarningAcknowledged) {
        this.priorityWarningAcknowledged = priorityWarningAcknowledged;
    }
}
