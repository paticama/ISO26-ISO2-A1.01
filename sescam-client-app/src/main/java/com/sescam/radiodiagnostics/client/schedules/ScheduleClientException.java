package com.sescam.radiodiagnostics.client.schedules;

import com.sescam.radiodiagnostics.common.schedules.AffectedAppointmentDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleSummaryDTO;

import java.util.List;

/**
 * Error returned by the schedules REST API, with the explicit error code of UC-AAD-02 and, for A1 and A2,
 * the conflicting schedules or the affected appointments to show to Admissions Staff.
 */
public class ScheduleClientException extends RuntimeException {

    /** Code used when the server cannot be reached. */
    public static final String SERVER_UNREACHABLE = "SERVER_UNREACHABLE";
    /** Code used when the server answered with an error without a known code. */
    public static final String GENERIC_ERROR = "GENERIC_ERROR";

    private final String errorCode;
    private final List<ScheduleSummaryDTO> conflictingSchedules;
    private final List<AffectedAppointmentDTO> affectedAppointments;

    public ScheduleClientException(String errorCode, String message, List<ScheduleSummaryDTO> conflictingSchedules,
                                   List<AffectedAppointmentDTO> affectedAppointments, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.conflictingSchedules = conflictingSchedules == null ? List.of() : List.copyOf(conflictingSchedules);
        this.affectedAppointments = affectedAppointments == null ? List.of() : List.copyOf(affectedAppointments);
    }

    public ScheduleClientException(String errorCode, String message) {
        this(errorCode, message, List.of(), List.of(), null);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public List<ScheduleSummaryDTO> getConflictingSchedules() {
        return conflictingSchedules;
    }

    public List<AffectedAppointmentDTO> getAffectedAppointments() {
        return affectedAppointments;
    }

    /** A2: the change needs the explicit confirmation of Admissions Staff. */
    public boolean needsConfirmation() {
        return "APPOINTMENTS_AFFECTED".equals(errorCode);
    }
}
