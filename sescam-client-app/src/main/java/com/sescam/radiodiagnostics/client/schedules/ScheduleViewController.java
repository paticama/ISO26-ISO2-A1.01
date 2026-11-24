package com.sescam.radiodiagnostics.client.schedules;

import com.sescam.radiodiagnostics.common.schedules.ScheduleChangeResultDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatus;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Map;

/**
 * Admissions UI of UC-AAD-02: search, open, update, close and reopen schedules.
 *
 * <p>A2 is a two-step dialogue: when the server answers {@code APPOINTMENTS_AFFECTED}, the view lists the
 * appointments and offers to repeat the same change with {@code confirmAffectedAppointments = true}.
 */
@Controller
@RequestMapping("/schedules")
public class ScheduleViewController {

    static final String LIST_VIEW = "schedules/list";
    static final String NEW_VIEW = "schedules/new";
    static final String DETAIL_VIEW = "schedules/detail";
    static final String DEFAULT_ACTOR = "staff-1";
    static final String DEFAULT_ROLE = "ADMISSIONS_STAFF";

    private final ScheduleApiClient apiClient;

    public ScheduleViewController(ScheduleApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /** Steps 1-2. */
    @GetMapping
    public String list(@RequestParam(value = "date", required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(value = "resourceId", required = false) String resourceId,
                       @RequestParam(value = "locationId", required = false) String locationId,
                       @RequestParam(value = "status", required = false) ScheduleStatus status,
                       Model model,
                       @RequestHeader(value = "X-Actor-Id", defaultValue = DEFAULT_ACTOR) String actorId,
                       @RequestHeader(value = "X-Actor-Role", defaultValue = DEFAULT_ROLE) String actorRole) {
        model.addAttribute("date", date);
        model.addAttribute("resourceId", resourceId);
        model.addAttribute("locationId", locationId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", ScheduleStatus.values());
        try {
            model.addAttribute("schedules", apiClient.search(
                    new ScheduleSearchCriteriaDTO(date, resourceId, locationId, status), actorId, actorRole));
        } catch (ScheduleClientException ex) {
            model.addAttribute("failure", ex.getMessage());
        }
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ScheduleForm());
        return NEW_VIEW;
    }

    /** Steps 3-7, open. */
    @PostMapping("/new")
    public String open(@ModelAttribute("form") ScheduleForm form, Model model, RedirectAttributes redirect,
                       @RequestHeader(value = "X-Actor-Id", defaultValue = DEFAULT_ACTOR) String actorId,
                       @RequestHeader(value = "X-Actor-Role", defaultValue = DEFAULT_ROLE) String actorRole) {
        Map<String, String> errors = form.validateOpen();
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            return NEW_VIEW;
        }
        try {
            ScheduleChangeResultDTO result = apiClient.open(form.toOpenRequest(actorId, actorRole));
            redirect.addFlashAttribute("success", "Schedule opened with " + result.schedule().slots().size()
                    + " slots.");
            return "redirect:/schedules/" + result.schedule().scheduleId();
        } catch (ScheduleClientException ex) {
            addFailure(model, ex);
            return NEW_VIEW;
        }
    }

    /** Step 2: one schedule with its slots, plus the edit, close and reopen forms. */
    @GetMapping("/{id}")
    public String detail(@PathVariable("id") String id, Model model,
                         @RequestHeader(value = "X-Actor-Id", defaultValue = DEFAULT_ACTOR) String actorId,
                         @RequestHeader(value = "X-Actor-Role", defaultValue = DEFAULT_ROLE) String actorRole) {
        try {
            ScheduleDTO schedule = apiClient.get(id, actorId, actorRole);
            model.addAttribute("schedule", schedule);
            if (!model.containsAttribute("form")) {
                model.addAttribute("form", ScheduleForm.fromDTO(schedule));
            }
        } catch (ScheduleClientException ex) {
            model.addAttribute("failure", ex.getMessage());
        }
        return DETAIL_VIEW;
    }

    /** Update (A1, A2, A5). */
    @PostMapping("/{id}/update")
    public String update(@PathVariable("id") String id, @ModelAttribute("form") ScheduleForm form, Model model,
                         RedirectAttributes redirect,
                         @RequestHeader(value = "X-Actor-Id", defaultValue = DEFAULT_ACTOR) String actorId,
                         @RequestHeader(value = "X-Actor-Role", defaultValue = DEFAULT_ROLE) String actorRole) {
        form.setScheduleId(id);
        Map<String, String> errors = form.validateWindow();
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            return detail(id, model, actorId, actorRole);
        }
        try {
            ScheduleChangeResultDTO result = apiClient.update(form.toUpdateRequest(actorId, actorRole));
            redirect.addFlashAttribute("success", successMessage("Schedule updated", result));
            redirect.addFlashAttribute("rescheduling", result.affectedAppointments());
            return "redirect:/schedules/" + id;
        } catch (ScheduleClientException ex) {
            addFailure(model, ex);
            model.addAttribute("pendingAction", "update");
            return detail(id, model, actorId, actorRole);
        }
    }

    /** Close (A2, A3). */
    @PostMapping("/{id}/close")
    public String close(@PathVariable("id") String id, @ModelAttribute("form") ScheduleForm form, Model model,
                        RedirectAttributes redirect,
                        @RequestHeader(value = "X-Actor-Id", defaultValue = DEFAULT_ACTOR) String actorId,
                        @RequestHeader(value = "X-Actor-Role", defaultValue = DEFAULT_ROLE) String actorRole) {
        form.setScheduleId(id);
        Map<String, String> errors = form.validateClose();
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            return detail(id, model, actorId, actorRole);
        }
        try {
            ScheduleChangeResultDTO result = apiClient.close(form.toStatusChangeRequest(actorId, actorRole));
            redirect.addFlashAttribute("success", successMessage("Schedule closed: no new bookings", result));
            redirect.addFlashAttribute("rescheduling", result.affectedAppointments());
            return "redirect:/schedules/" + id;
        } catch (ScheduleClientException ex) {
            addFailure(model, ex);
            model.addAttribute("pendingAction", "close");
            return detail(id, model, actorId, actorRole);
        }
    }

    /** Reopen (A4). */
    @PostMapping("/{id}/reopen")
    public String reopen(@PathVariable("id") String id, @ModelAttribute("form") ScheduleForm form, Model model,
                         RedirectAttributes redirect,
                         @RequestHeader(value = "X-Actor-Id", defaultValue = DEFAULT_ACTOR) String actorId,
                         @RequestHeader(value = "X-Actor-Role", defaultValue = DEFAULT_ROLE) String actorRole) {
        form.setScheduleId(id);
        try {
            apiClient.reopen(form.toStatusChangeRequest(actorId, actorRole));
            redirect.addFlashAttribute("success", "Schedule reopened: its slots are bookable again.");
            return "redirect:/schedules/" + id;
        } catch (ScheduleClientException ex) {
            addFailure(model, ex);
            return detail(id, model, actorId, actorRole);
        }
    }

    private static void addFailure(Model model, ScheduleClientException ex) {
        model.addAttribute("failure", ex.getMessage());
        model.addAttribute("errorCode", ex.getErrorCode());
        model.addAttribute("conflicts", ex.getConflictingSchedules());
        model.addAttribute("affected", ex.getAffectedAppointments());
        model.addAttribute("needsConfirmation", ex.needsConfirmation());
    }

    private static String successMessage(String prefix, ScheduleChangeResultDTO result) {
        return result.reschedulingRequired()
                ? prefix + ". " + result.affectedAppointments().size()
                  + " appointment(s) were handed to admissions for rescheduling."
                : prefix + ".";
    }
}
