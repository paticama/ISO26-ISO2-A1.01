package com.sescam.radiodiagnostics.client.admissions;

import com.sescam.radiodiagnostics.common.admissions.AppointmentAction;
import com.sescam.radiodiagnostics.common.admissions.AppointmentDetailsDTO;
import com.sescam.radiodiagnostics.common.admissions.AppointmentSummaryDTO;
import com.sescam.radiodiagnostics.common.orders.PriorityLevel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/** Patient portal views and form handling for UC-PAT-01. */
@Controller
@RequestMapping("/appointments")
public class PatientAppointmentViewController {

    static final String LIST_VIEW = "appointments/index";
    static final String DETAIL_VIEW = "appointments/detail";

    private final PatientAppointmentApiClient apiClient;

    public PatientAppointmentViewController(PatientAppointmentApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @GetMapping
    public String list(@RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                       @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
                       Model model) {
        if (!isPatient(actorId, actorRole)) {
            model.addAttribute("failure", "Sign in as a patient to view your appointments.");
            model.addAttribute("appointments", List.of());
            return LIST_VIEW;
        }
        try {
            List<AppointmentSummaryDTO> appointments = apiClient.list(actorId, actorRole);
            model.addAttribute("appointments", appointments);
            model.addAttribute("empty", appointments.isEmpty());
        } catch (BookingClientException ex) {
            model.addAttribute("failure", ex.getMessage());
            model.addAttribute("appointments", List.of());
        }
        return LIST_VIEW;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") String id,
                         @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                         @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
                         Model model) {
        model.addAttribute("form", new PatientAppointmentForm());
        if (!isPatient(actorId, actorRole)) {
            model.addAttribute("failure", "Sign in as a patient to view appointment details.");
            return DETAIL_VIEW;
        }
        loadDetails(id, actorId, actorRole, model);
        return DETAIL_VIEW;
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable("id") String id,
                          @RequestParam("expectedBookingVersion") long expectedBookingVersion,
                          @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                          @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
                          RedirectAttributes redirect) {
        if (!isPatient(actorId, actorRole)) {
            redirect.addFlashAttribute("failure", "Sign in as a patient to confirm an appointment.");
            return "redirect:/appointments/" + id;
        }
        if (expectedBookingVersion < 0) {
            redirect.addFlashAttribute("failure", "The appointment version is invalid. Reload and try again.");
            return "redirect:/appointments/" + id;
        }
        try {
            apiClient.confirm(id, expectedBookingVersion, actorId, actorRole);
            redirect.addFlashAttribute("success", "Your appointment has been confirmed.");
        } catch (BookingClientException ex) {
            redirect.addFlashAttribute("failure", ex.getMessage());
        }
        return "redirect:/appointments/" + id;
    }

    @PostMapping("/{id}/reschedule-requests")
    public String requestRescheduling(@PathVariable("id") String id,
                                      @ModelAttribute("form") PatientAppointmentForm form,
                                      @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                                      @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
                                      Model model, RedirectAttributes redirect) {
        if (!isPatient(actorId, actorRole)) {
            redirect.addFlashAttribute("failure", "Sign in as a patient to request a reschedule.");
            return "redirect:/appointments/" + id;
        }
        Map<String, String> errors = form.validateReschedule();
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("form", form);
            loadDetails(id, actorId, actorRole, model);
            return DETAIL_VIEW;
        }
        try {
            apiClient.requestRescheduling(id, form.getReason().trim(), form.parsedPreferredDates(), actorId, actorRole);
            redirect.addFlashAttribute("success", "Your rescheduling request was sent to Admissions; your appointment has not changed.");
        } catch (BookingClientException ex) {
            // Redirecting reloads details so a duplicate request (A6) shows its existing status.
            redirect.addFlashAttribute("failure", ex.getMessage());
        }
        return "redirect:/appointments/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable("id") String id,
                         @RequestParam("expectedBookingVersion") long expectedBookingVersion,
                         @ModelAttribute("form") PatientAppointmentForm form,
                         @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                         @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
                         Model model, RedirectAttributes redirect) {
        if (!isPatient(actorId, actorRole)) {
            redirect.addFlashAttribute("failure", "Sign in as a patient to cancel an appointment.");
            return "redirect:/appointments/" + id;
        }
        if (expectedBookingVersion < 0) {
            redirect.addFlashAttribute("failure", "The appointment version is invalid. Reload and try again.");
            return "redirect:/appointments/" + id;
        }
        try {
            AppointmentDetailsDTO details = apiClient.get(id, actorId, actorRole);
            boolean priority = isPriority(details.priority());
            Map<String, String> errors = form.validateCancellation(priority);
            if (!errors.isEmpty()) {
                model.addAttribute("errors", errors);
                model.addAttribute("form", form);
                addDetailsToModel(details, model);
                return DETAIL_VIEW;
            }
            apiClient.cancel(id, expectedBookingVersion, form.getReason(),
                    form.isPriorityWarningAcknowledged(), actorId, actorRole);
            redirect.addFlashAttribute("success", "Your appointment was cancelled and Admissions was informed.");
        } catch (BookingClientException ex) {
            redirect.addFlashAttribute("failure", ex.getMessage());
        }
        return "redirect:/appointments/" + id;
    }

    private void loadDetails(String id, String actorId, String actorRole, Model model) {
        try {
            addDetailsToModel(apiClient.get(id, actorId, actorRole), model);
        } catch (BookingClientException ex) {
            model.addAttribute("failure", ex.getMessage());
        }
    }

    private static void addDetailsToModel(AppointmentDetailsDTO details, Model model) {
        model.addAttribute("appointment", details);
        model.addAttribute("priorityExamination", isPriority(details.priority()));
        model.addAttribute("canConfirm", details.allowedActions().contains(AppointmentAction.CONFIRM));
        model.addAttribute("canReschedule", details.allowedActions().contains(AppointmentAction.REQUEST_RESCHEDULE));
        model.addAttribute("canCancel", details.allowedActions().contains(AppointmentAction.CANCEL));
    }

    private static boolean isPatient(String actorId, String actorRole) {
        return actorId != null && !actorId.isBlank() && "PATIENT".equalsIgnoreCase(actorRole);
    }

    private static boolean isPriority(PriorityLevel priority) {
        return priority == PriorityLevel.STAT_URGENT || priority == PriorityLevel.PREFERENTIAL;
    }
}
