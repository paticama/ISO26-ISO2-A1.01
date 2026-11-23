package com.sescam.radiodiagnostics.client.patients;

import com.sescam.radiodiagnostics.common.patients.Gender;
import com.sescam.radiodiagnostics.common.patients.PatientDTO;
import com.sescam.radiodiagnostics.common.patients.PatientStatus;
import com.sescam.radiodiagnostics.common.patients.PreferredNotificationChannel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Controller for Patient Management in the client app (UC-AAD-01).
 */
@Controller
@RequestMapping("/patients")
public class PatientViewController {

    static final String LIST_VIEW = "patients/list";
    static final String REGISTER_VIEW = "patients/register";
    static final String DETAIL_VIEW = "patients/detail";

    private final PatientApiClient apiClient;

    public PatientViewController(PatientApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @GetMapping
    public String list(
            @RequestParam(value = "query", defaultValue = "") String query,
            @RequestParam(value = "status", required = false) PatientStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Model model,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "staff-1") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "ADMINISTRATIVE_STAFF") String actorRole) {
        try {
            model.addAttribute("patients", apiClient.search(query, status, page, size, actorId, actorRole));
            model.addAttribute("query", query);
            model.addAttribute("selectedStatus", status);
        } catch (PatientClientException ex) {
            model.addAttribute("failure", ex.getMessage());
        }
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String showRegistrationForm(Model model) {
        model.addAttribute("form", new PatientForm());
        model.addAttribute("genders", Gender.values());
        model.addAttribute("channels", PreferredNotificationChannel.values());
        return REGISTER_VIEW;
    }

    @PostMapping("/new")
    public String register(
            @ModelAttribute("form") PatientForm form,
            Model model,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "staff-1") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "ADMINISTRATIVE_STAFF") String actorRole) {
        Map<String, String> errors = form.validateRegistration();
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("genders", Gender.values());
            model.addAttribute("channels", PreferredNotificationChannel.values());
            return REGISTER_VIEW;
        }

        try {
            PatientDTO created = apiClient.register(form.toCreateRequest(actorId, actorRole));
            return "redirect:/patients/" + created.id();
        } catch (PatientClientException ex) {
            model.addAttribute("failure", ex.getMessage());
            model.addAttribute("serverErrors", ex.getErrors());
            model.addAttribute("genders", Gender.values());
            model.addAttribute("channels", PreferredNotificationChannel.values());
            return REGISTER_VIEW;
        }
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable("id") String id,
            Model model,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "staff-1") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "ADMINISTRATIVE_STAFF") String actorRole) {
        try {
            PatientDTO patient = apiClient.getById(id, actorId, actorRole);
            model.addAttribute("patient", patient);
            model.addAttribute("form", PatientForm.fromDTO(patient));
            model.addAttribute("channels", PreferredNotificationChannel.values());
        } catch (PatientClientException ex) {
            model.addAttribute("failure", ex.getMessage());
            PatientForm fallback = new PatientForm();
            fallback.setId(id);
            fallback.setFirstName("Not");
            fallback.setLastName("Found");
            fallback.setStatus(PatientStatus.SUSPENDED);
            model.addAttribute("form", fallback);
            model.addAttribute("channels", PreferredNotificationChannel.values());
        }
        return DETAIL_VIEW;
    }

    @PostMapping("/{id}/update")
    public String update(
            @PathVariable("id") String id,
            @ModelAttribute("form") PatientForm form,
            Model model,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "staff-1") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "ADMINISTRATIVE_STAFF") String actorRole) {
        form.setId(id);
        Map<String, String> errors = form.validateUpdate();
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            return detail(id, model, actorId, actorRole);
        }

        try {
            apiClient.update(form.toUpdateRequest(actorId, actorRole));
            return "redirect:/patients/" + id;
        } catch (PatientClientException ex) {
            model.addAttribute("failure", ex.getMessage());
            return detail(id, model, actorId, actorRole);
        }
    }

    @PostMapping("/{id}/discharge")
    public String discharge(
            @PathVariable("id") String id,
            @ModelAttribute("form") PatientForm form,
            Model model,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "staff-1") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "ADMINISTRATIVE_STAFF") String actorRole) {
        form.setId(id);
        try {
            apiClient.discharge(form.toDischargeRequest(actorId, actorRole));
            return "redirect:/patients/" + id;
        } catch (PatientClientException ex) {
            model.addAttribute("failure", ex.getMessage());
            return detail(id, model, actorId, actorRole);
        }
    }
}
