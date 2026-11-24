package com.sescam.radiodiagnostics.client.security;

import es.uclm.esi.sescam.security.api.RoleAssignmentDTO;
import es.uclm.esi.sescam.security.api.RoleChangeRequestDTO;
import es.uclm.esi.sescam.security.api.RoleChangeResultDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * View of UC-ADM-02 in the client app: an administrator looks up a user, changes the user's role and confirms.
 *
 * <p>Flow: {@code lookup} (steps 1-2) → {@code validate} (steps 3-4, nothing saved) → the page shows the
 * resulting access and a confirm button (step 5) → {@code confirm} (step 6). The actor identity comes from the
 * {@code X-Actor-Id} header until the login of the client is available, as in the other views.
 */
@Controller
@RequestMapping("/security/roles")
public class RoleAdminViewController {

    static final String VIEW = "security/roles";

    private final RoleAdminApiClient apiClient;

    public RoleAdminViewController(RoleAdminApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /** Opens the role administration area with an empty form. */
    @GetMapping
    public String open(Model model) {
        model.addAttribute("form", new RoleChangeForm());
        return VIEW;
    }

    /** Steps 1-2: show the current role assignments and the account status. */
    @GetMapping("/lookup")
    public String lookup(@ModelAttribute("form") RoleChangeForm form, Model model,
                         @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId) {
        Map<String, String> errors = form.validate(false);
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            return VIEW;
        }
        try {
            RoleAssignmentDTO assignment = apiClient.getRoleAssignments(form.getUserId(), actorId);
            model.addAttribute("assignment", assignment);
        } catch (RoleAdminClientException ex) {
            model.addAttribute("failure", ex);
        }
        return VIEW;
    }

    /** Steps 3-4: validate the requested role; the page then asks for confirmation. */
    @PostMapping("/validate")
    public String validate(@ModelAttribute("form") RoleChangeForm form, Model model,
                           @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId) {
        return change(form, model, actorId, false);
    }

    /** Steps 5-6: save the change the administrator confirmed. */
    @PostMapping("/confirm")
    public String confirm(@ModelAttribute("form") RoleChangeForm form, Model model,
                          @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId) {
        return change(form, model, actorId, true);
    }

    private String change(RoleChangeForm form, Model model, String actorId, boolean confirm) {
        Map<String, String> errors = form.validate(true);
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            return VIEW;
        }
        RoleChangeRequestDTO request = new RoleChangeRequestDTO(actorId, form.getUserId(), form.getRoleName());
        try {
            RoleChangeResultDTO result = confirm
                    ? apiClient.confirmRoleChange(request)
                    : apiClient.validateRoleChange(request);
            model.addAttribute("result", result);
        } catch (RoleAdminClientException ex) {
            model.addAttribute("failure", ex);
        }
        return VIEW;
    }
}
