package com.sescam.radiodiagnostics.common.schedules;

import java.util.List;

/**
 * A2: the change would reduce availability that existing appointments use. Nothing is saved; the affected
 * appointments are listed so Admissions Staff can review them and repeat the request with
 * {@code confirmAffectedAppointments = true}.
 */
public class AppointmentsAffectedException extends ScheduleException {

    private final List<AffectedAppointmentDTO> affectedAppointments;

    public AppointmentsAffectedException(String message, List<AffectedAppointmentDTO> affectedAppointments) {
        super(ScheduleErrorCode.APPOINTMENTS_AFFECTED, message);
        this.affectedAppointments = affectedAppointments == null ? List.of() : List.copyOf(affectedAppointments);
    }

    public List<AffectedAppointmentDTO> getAffectedAppointments() {
        return affectedAppointments;
    }
}
