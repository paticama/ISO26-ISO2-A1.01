package com.sescam.radiodiagnostics.server.schedules;

import com.sescam.radiodiagnostics.common.schedules.AppointmentsAffectedException;
import com.sescam.radiodiagnostics.common.schedules.ScheduleException;
import com.sescam.radiodiagnostics.common.schedules.ScheduleOverlapException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps the UC-AAD-02 exceptions to HTTP. The body always carries the explicit error code of the
 * specification in {@code error}; A1 adds {@code conflictingSchedules} and A2 adds {@code affectedAppointments}.
 */
@RestControllerAdvice(assignableTypes = ScheduleRestController.class)
public class ScheduleExceptionHandler {

    @ExceptionHandler(ScheduleException.class)
    public ResponseEntity<Map<String, Object>> handle(ScheduleException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getCode().name());
        body.put("message", ex.getMessage());
        if (ex instanceof ScheduleOverlapException overlap) {
            body.put("conflictingSchedules", overlap.getConflictingSchedules());
        }
        if (ex instanceof AppointmentsAffectedException affected) {
            body.put("affectedAppointments", affected.getAffectedAppointments());
        }
        return ResponseEntity.status(statusOf(ex)).body(body);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> handleUnreadable(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "INVALID_SCHEDULE",
                "message", "The request is malformed: check dates (yyyy-MM-ddTHH:mm) and numbers."));
    }

    static HttpStatus statusOf(ScheduleException ex) {
        return switch (ex.getCode()) {
            case INVALID_SCHEDULE -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED_OPERATION -> HttpStatus.FORBIDDEN;
            case SCHEDULE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CAPACITY_EXCEEDED -> HttpStatus.UNPROCESSABLE_ENTITY;
            case SCHEDULE_OVERLAP, APPOINTMENTS_AFFECTED, CONCURRENT_MODIFICATION, SLOT_UNAVAILABLE -> HttpStatus.CONFLICT;
        };
    }
}
