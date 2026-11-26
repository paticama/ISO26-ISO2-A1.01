package com.sescam.radiodiagnostics.server.admissions;

import com.sescam.radiodiagnostics.common.admissions.BookingDTO;
import com.sescam.radiodiagnostics.common.admissions.BookingService;
import com.sescam.radiodiagnostics.common.admissions.CancelBookingRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.CreateBookingRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.PendingExamRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.RescheduleBookingRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.WorklistItemDTO;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST API for UC-AAD-05: Book Requested Examination (C5 v1.0.0).
 */
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingRestController {

    private final BookingService bookingService;

    public BookingRestController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/pending-orders")
    public ResponseEntity<List<PendingExamRequestDTO>> listPending(
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.ok(bookingService.listPendingRequests(actorId, actorRole));
    }

    @GetMapping("/slots")
    public ResponseEntity<List<SlotDTO>> findCompatibleSlots(
            @RequestParam("orderId") String orderId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.ok(bookingService.findCompatibleSlots(orderId, from, to, actorId, actorRole));
    }

    @PostMapping
    public ResponseEntity<BookingDTO> createBooking(
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestBody CreateBookingPayload payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(
                new CreateBookingRequestDTO(payload.orderId(), payload.slotId(),
                        payload.expectedSlotVersion(), actorId, actorRole)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingDTO> getBooking(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.ok(bookingService.getBooking(id, actorId, actorRole));
    }

    @PostMapping("/{id}/reschedule")
    public ResponseEntity<BookingDTO> rescheduleBooking(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestBody ReschedulePayload payload) {
        return ResponseEntity.ok(bookingService.rescheduleBooking(
                new RescheduleBookingRequestDTO(id, payload.newSlotId(), payload.expectedSlotVersion(),
                        payload.expectedBookingVersion(), payload.reason(), actorId, actorRole)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingDTO> cancelBooking(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestBody CancelPayload payload) {
        return ResponseEntity.ok(bookingService.cancelBooking(
                new CancelBookingRequestDTO(id, payload.expectedBookingVersion(), payload.reason(), actorId, actorRole)));
    }

    @GetMapping("/worklist")
    public ResponseEntity<List<WorklistItemDTO>> listWorklist(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.ok(bookingService.listDailyWorklist(date, actorId, actorRole));
    }

    public record CreateBookingPayload(String orderId, String slotId, long expectedSlotVersion) {}

    public record ReschedulePayload(String newSlotId, long expectedSlotVersion, long expectedBookingVersion, String reason) {}

    public record CancelPayload(long expectedBookingVersion, String reason) {}
}
