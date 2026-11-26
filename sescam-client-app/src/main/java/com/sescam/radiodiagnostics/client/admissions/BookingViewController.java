package com.sescam.radiodiagnostics.client.admissions;

import com.sescam.radiodiagnostics.common.admissions.BookingDTO;
import com.sescam.radiodiagnostics.common.admissions.PendingExamRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.WorklistItemDTO;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Admissions UI controller for UC-AAD-05: Book Requested Examination.
 */
@Controller
@RequestMapping("/bookings")
public class BookingViewController {

    static final String INDEX_VIEW = "bookings/index";
    static final String DETAIL_VIEW = "bookings/detail";

    private final BookingApiClient apiClient;

    public BookingViewController(BookingApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @GetMapping
    public String index(
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            Model model) {
        BookingForm form = new BookingForm();
        form.setOrderId(orderId);
        prepareIndex(model, form, orderId, date, actorId, actorRole);
        return INDEX_VIEW;
    }

    @PostMapping
    public String createBooking(
            @ModelAttribute("form") BookingForm form,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            Model model) {
        Map<String, String> errors = form.validate();
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            prepareIndex(model, form, form.getOrderId(), null, actorId, actorRole);
            return INDEX_VIEW;
        }
        try {
            BookingDTO created = apiClient.createBooking(form.toPayload(), actorId, actorRole);
            return "redirect:/bookings/" + created.bookingId();
        } catch (BookingClientException ex) {
            model.addAttribute("failure", ex);
            prepareIndex(model, form, form.getOrderId(), null, actorId, actorRole);
            return INDEX_VIEW;
        }
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            Model model) {
        try {
            BookingDTO booking = apiClient.getBooking(id, actorId, actorRole);
            model.addAttribute("booking", booking);
        } catch (BookingClientException ex) {
            model.addAttribute("failure", ex);
        }
        return DETAIL_VIEW;
    }

    @PostMapping("/{id}/reschedule")
    public String reschedule(
            @PathVariable("id") String id,
            @RequestParam("newSlotId") String newSlotId,
            @RequestParam("expectedSlotVersion") long expectedSlotVersion,
            @RequestParam("expectedBookingVersion") long expectedBookingVersion,
            @RequestParam("reason") String reason,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            Model model) {
        try {
            BookingDTO updated = apiClient.rescheduleBooking(id,
                    new BookingApiClient.ReschedulePayload(newSlotId, expectedSlotVersion, expectedBookingVersion, reason),
                    actorId, actorRole);
            model.addAttribute("booking", updated);
        } catch (BookingClientException ex) {
            model.addAttribute("failure", ex);
            loadBookingSafe(id, actorId, actorRole, model);
        }
        return DETAIL_VIEW;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(
            @PathVariable("id") String id,
            @RequestParam("expectedBookingVersion") long expectedBookingVersion,
            @RequestParam("reason") String reason,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            Model model) {
        try {
            BookingDTO cancelled = apiClient.cancelBooking(id,
                    new BookingApiClient.CancelPayload(expectedBookingVersion, reason),
                    actorId, actorRole);
            model.addAttribute("booking", cancelled);
        } catch (BookingClientException ex) {
            model.addAttribute("failure", ex);
            loadBookingSafe(id, actorId, actorRole, model);
        }
        return DETAIL_VIEW;
    }

    private void prepareIndex(Model model, BookingForm form, String orderId, LocalDate date,
                              String actorId, String actorRole) {
        model.addAttribute("form", form);
        try {
            List<PendingExamRequestDTO> pending = apiClient.listPendingOrders(actorId, actorRole);
            model.addAttribute("pendingOrders", pending);
            List<SlotDTO> slots = (orderId != null && !orderId.isBlank())
                    ? apiClient.findCompatibleSlots(orderId.trim(), actorId, actorRole)
                    : List.of();
            model.addAttribute("slots", slots);
            List<WorklistItemDTO> worklist = apiClient.listWorklist(date, actorId, actorRole);
            model.addAttribute("worklist", worklist);
        } catch (BookingClientException ex) {
            model.addAttribute("pendingOrders", List.of());
            model.addAttribute("slots", List.of());
            model.addAttribute("worklist", List.of());
            model.addAttribute("failure", ex);
        }
    }

    private void loadBookingSafe(String id, String actorId, String actorRole, Model model) {
        try {
            model.addAttribute("booking", apiClient.getBooking(id, actorId, actorRole));
        } catch (BookingClientException ignored) {
            // Original failure is already attached to model.
        }
    }
}
