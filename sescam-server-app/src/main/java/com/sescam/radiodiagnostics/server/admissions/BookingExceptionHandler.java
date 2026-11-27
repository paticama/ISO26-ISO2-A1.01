package com.sescam.radiodiagnostics.server.admissions;

import com.sescam.radiodiagnostics.common.admissions.BookingConflictException;
import com.sescam.radiodiagnostics.common.admissions.BookingException;
import com.sescam.radiodiagnostics.common.admissions.BookingNotFoundException;
import com.sescam.radiodiagnostics.common.admissions.ConcurrentBookingException;
import com.sescam.radiodiagnostics.common.admissions.DuplicatePendingRequestException;
import com.sescam.radiodiagnostics.common.admissions.InvalidBookingDataException;
import com.sescam.radiodiagnostics.common.admissions.PatientNotEligibleException;
import com.sescam.radiodiagnostics.common.admissions.RequestAlreadyProcessedException;
import com.sescam.radiodiagnostics.common.admissions.SlotUnavailableBookingException;
import com.sescam.radiodiagnostics.common.admissions.UnauthorizedBookingAccessException;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Maps the booking exceptions of C5 to explicit HTTP status codes and payloads, for the staff API (UC-AAD-05)
 * and the patient API (UC-PAT-01), which share the same exceptions.
 */
@RestControllerAdvice(assignableTypes = {BookingRestController.class, PatientAppointmentRestController.class})
public class BookingExceptionHandler {

    @ExceptionHandler(InvalidBookingDataException.class)
    public ResponseEntity<BookingErrorResponse> handleInvalid(InvalidBookingDataException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new BookingErrorResponse(ex.getCode().name(), ex.getMessage(), ex.getInvalidFields(), List.of()));
    }

    @ExceptionHandler(UnauthorizedBookingAccessException.class)
    public ResponseEntity<BookingErrorResponse> handleUnauthorized(UnauthorizedBookingAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new BookingErrorResponse(ex.getCode().name(), ex.getMessage(), List.of(), List.of()));
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<BookingErrorResponse> handleNotFound(BookingNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new BookingErrorResponse(ex.getCode().name(), ex.getMessage(), List.of(), List.of()));
    }

    @ExceptionHandler(SlotUnavailableBookingException.class)
    public ResponseEntity<BookingErrorResponse> handleSlotUnavailable(SlotUnavailableBookingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new BookingErrorResponse(ex.getCode().name(), ex.getMessage(), List.of(), ex.getAlternativeSlots()));
    }

    @ExceptionHandler({
            RequestAlreadyProcessedException.class,
            BookingConflictException.class,
            ConcurrentBookingException.class,
            DuplicatePendingRequestException.class
    })
    public ResponseEntity<BookingErrorResponse> handleConflict(BookingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new BookingErrorResponse(ex.getCode().name(), ex.getMessage(), List.of(), List.of()));
    }

    @ExceptionHandler(PatientNotEligibleException.class)
    public ResponseEntity<BookingErrorResponse> handleUnprocessable(PatientNotEligibleException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new BookingErrorResponse(ex.getCode().name(), ex.getMessage(), List.of(), List.of()));
    }

    public record BookingErrorResponse(
            String code,
            String message,
            List<String> invalidFields,
            List<SlotDTO> alternativeSlots
    ) {
    }
}
