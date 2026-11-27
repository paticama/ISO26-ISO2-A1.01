package com.sescam.radiodiagnostics.client.admissions;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientAppointmentFormTest {

    @Test
    void rescheduleRequiresReasonAndValidatesPreferredDatesBeforePayloadConversion() {
        PatientAppointmentForm form = new PatientAppointmentForm();
        form.setPreferredDates(LocalDate.now().plusDays(1) + ", " + LocalDate.now().plusDays(3));
        assertTrue(form.validateReschedule().containsKey("reason"));

        form.setReason("Work conflict");
        assertTrue(form.validateReschedule().isEmpty());
        assertEquals(2, form.parsedPreferredDates().size());

        form.setPreferredDates("not-a-date");
        assertTrue(form.validateReschedule().containsKey("preferredDates"));
    }

    @Test
    void rejectsPreferredDatesInPast() {
        PatientAppointmentForm form = new PatientAppointmentForm();
        form.setReason("Need another day");
        form.setPreferredDates(LocalDate.now().minusDays(1).toString());
        assertTrue(form.validateReschedule().containsKey("preferredDates"));
    }

    @Test
    void cancellationRequiresExplicitConfirmationAndPriorityAcknowledgementWhenNeeded() {
        PatientAppointmentForm form = new PatientAppointmentForm();
        assertTrue(form.validateCancellation(true).containsKey("cancellationConfirmed"));
        assertTrue(form.validateCancellation(true).containsKey("priorityWarningAcknowledged"));

        form.setCancellationConfirmed(true);
        form.setPriorityWarningAcknowledged(true);
        assertTrue(form.validateCancellation(true).isEmpty());
        assertFalse(form.validateCancellation(false).containsKey("priorityWarningAcknowledged"));
    }
}
