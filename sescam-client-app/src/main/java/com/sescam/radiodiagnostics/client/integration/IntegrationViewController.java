package com.sescam.radiodiagnostics.client.integration;

import com.sescam.radiodiagnostics.common.integration.IntegrationSummaryDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * View of UC-ADM-04 in the client app: the integrations area of the system administrator.
 * <p>
 * Main flow: {@code GET /integrations} (steps 1-2), {@code GET /integrations/{id}} (step 3),
 * {@code POST .../validate} (step 5), {@code POST .../save} (steps 6-7). {@code POST .../check} is the
 * retry of A3. The actor identity comes from the {@code X-Actor-Id}/{@code X-Actor-Role} headers until the
 * login of UC-ADM-02 is available in the client.
 * <p>
 * The credential is never put back in the page (A4): after every request it is removed from the form.
 */
@Controller
@RequestMapping("/integrations")
public class IntegrationViewController {

    static final String LIST_VIEW = "integrations/list";
    static final String EDIT_VIEW = "integrations/edit";

    private final IntegrationApiClient apiClient;

    public IntegrationViewController(IntegrationApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /** Steps 1-2: open the integrations area and show every integration with its status. */
    @GetMapping
    public String list(Model model,
                       @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                       @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        try {
            model.addAttribute("integrations", apiClient.list(actorId, actorRole));
        } catch (IntegrationClientException ex) {
            model.addAttribute("failure", ex);
        }
        return LIST_VIEW;
    }

    /** Step 3: select an integration to configure or review. */
    @GetMapping("/{id}")
    public String edit(@PathVariable("id") String id, Model model,
                       @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                       @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        IntegrationSettingsForm form = new IntegrationSettingsForm();
        IntegrationSummaryDTO integration = load(id, actorId, actorRole, model);
        if (integration != null) {
            form.setEndpointUrl(integration.endpointUrl());
            form.setUsername(integration.username());
            form.setExpectedVersion(integration.version());
        }
        model.addAttribute("form", form);
        return EDIT_VIEW;
    }

    /** Step 5: check the typed settings and the connection with them. Nothing is saved. */
    @PostMapping("/{id}/validate")
    public String validate(@PathVariable("id") String id, @ModelAttribute("form") IntegrationSettingsForm form,
                           Model model,
                           @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                           @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        IntegrationSummaryDTO integration = load(id, actorId, actorRole, model);
        if (integration != null && rejectLocally(form, integration, model)) {
            return EDIT_VIEW;
        }
        if (integration != null) {
            try {
                model.addAttribute("check", apiClient.validate(id, form.toSettings(), actorId, actorRole));
            } catch (IntegrationClientException ex) {
                model.addAttribute("failure", ex);
            }
        }
        form.clearSecret();
        return EDIT_VIEW;
    }

    /** Steps 6-7: the administrator confirmed; save and report the status. */
    @PostMapping("/{id}/save")
    public String save(@PathVariable("id") String id, @ModelAttribute("form") IntegrationSettingsForm form,
                       Model model,
                       @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                       @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        IntegrationSummaryDTO integration = load(id, actorId, actorRole, model);
        if (integration != null && rejectLocally(form, integration, model)) {
            return EDIT_VIEW;
        }
        if (integration != null) {
            try {
                model.addAttribute("saved", apiClient.update(id, form.toSettings(), form.getExpectedVersion(),
                        actorId, actorRole));
                // show the new saved state and use its version for the next change
                IntegrationSummaryDTO refreshed = load(id, actorId, actorRole, model);
                if (refreshed != null) {
                    form.setExpectedVersion(refreshed.version());
                }
            } catch (IntegrationClientException ex) {
                model.addAttribute("failure", ex);
            }
        }
        form.clearSecret();
        return EDIT_VIEW;
    }

    /** A3: check the saved configuration again (retry or diagnostic review). */
    @PostMapping("/{id}/check")
    public String check(@PathVariable("id") String id, @ModelAttribute("form") IntegrationSettingsForm form,
                        Model model,
                        @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                        @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        try {
            model.addAttribute("result", apiClient.check(id, actorId, actorRole));
        } catch (IntegrationClientException ex) {
            model.addAttribute("failure", ex);
        }
        IntegrationSummaryDTO integration = load(id, actorId, actorRole, model);
        if (integration != null) {
            form.setEndpointUrl(integration.endpointUrl());
            form.setUsername(integration.username());
            form.setExpectedVersion(integration.version());
        }
        form.clearSecret();
        return EDIT_VIEW;
    }

    /** Loads the saved integration for display; on failure the error is put in the model. */
    private IntegrationSummaryDTO load(String id, String actorId, String actorRole, Model model) {
        try {
            IntegrationSummaryDTO integration = apiClient.get(id, actorId, actorRole);
            model.addAttribute("integration", integration);
            return integration;
        } catch (IntegrationClientException ex) {
            if (!model.containsAttribute("failure")) {
                model.addAttribute("failure", ex);
            }
            return null;
        }
    }

    /** Field validations before calling the server. Returns true when the form was rejected. */
    private boolean rejectLocally(IntegrationSettingsForm form, IntegrationSummaryDTO integration, Model model) {
        Map<String, String> errors = form.validate(integration.credentialConfigured());
        if (errors.isEmpty()) {
            return false;
        }
        model.addAttribute("errors", errors);
        form.clearSecret();
        return true;
    }
}
