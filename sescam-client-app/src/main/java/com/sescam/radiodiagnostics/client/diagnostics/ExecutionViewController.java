package com.sescam.radiodiagnostics.client.diagnostics;

import com.sescam.radiodiagnostics.common.diagnostics.ExecutionRecordDTO;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentSeverity;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * View of UC-PHY-05 in the client app: log the execution of a booked procedure, with an incident (2a) and the
 * option to mark it as aborted (2b). The actor comes from the {@code X-Actor-Id}/{@code X-Actor-Role} headers until
 * the client has login, as in the other views.
 */
@Controller
@RequestMapping("/executions")
public class ExecutionViewController {

    static final String VIEW = "executions/new";

    private final ExecutionApiClient apiClient;
    private final ZoneId zone;

    public ExecutionViewController(ExecutionApiClient apiClient, @Value("${sescam.client.zone}") String zone) {
        this.apiClient = apiClient;
        this.zone = ZoneId.of(zone);
    }

    /** Step 1: procedures awaiting execution and an empty form. */
    @GetMapping("/new")
    public String newExecution(Model model,
                               @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                               @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        prepare(model, new ExecutionForm(), actorId, actorRole);
        return VIEW;
    }

    /** Steps 2-5, 2a and 2b. */
    @PostMapping
    public String submit(@ModelAttribute("form") ExecutionForm form, Model model,
                         @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                         @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        Map<String, String> errors = form.validate();
        if (!errors.isEmpty()) {
            prepare(model, form, actorId, actorRole);
            model.addAttribute("errors", errors);
            return VIEW;
        }
        try {
            ExecutionRecordDTO record = apiClient.recordExecution(form.toPayload(zone), actorId, actorRole);
            model.addAttribute("form", form);
            model.addAttribute("record", record);
            model.addAttribute("zone", zone);
        } catch (ExecutionClientException ex) {
            prepare(model, form, actorId, actorRole);
            model.addAttribute("failure", ex);
            if (ex.getKind() == ExecutionClientException.Kind.INVALID_DATA) {
                model.addAttribute("errors", serverFieldErrors(ex.getInvalidFields()));
            }
        }
        return VIEW;
    }

    private void prepare(Model model, ExecutionForm form, String actorId, String actorRole) {
        model.addAttribute("form", form);
        model.addAttribute("zone", zone);
        model.addAttribute("incidentTypes", IncidentType.values());
        model.addAttribute("severities", IncidentSeverity.values());
        try {
            model.addAttribute("procedures", apiClient.listPending(actorId, actorRole));
        } catch (ExecutionClientException ex) {
            model.addAttribute("procedures", List.of());
            model.addAttribute("pendingFailure", ex.getMessage());
        }
    }

    /** Maps the server's field names to the form's; incident fields go to the single incident of the form. */
    static Map<String, String> serverFieldErrors(List<String> fields) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (String field : fields) {
            String formField = switch (field) {
                case "incidents" -> "incidentDescription";
                default -> field.startsWith("incidents[")
                        ? "incident" + capitalize(field.substring(field.indexOf("].") + 2))
                        : field;
            };
            errors.put(formField, switch (formField) {
                case "informedConsentVerified" -> "This examination requires verified informed consent.";
                case "doseDlp", "doseCtdiVol" -> "Mandatory for CT examinations.";
                case "radiotracerActivityMBq" -> "Mandatory for nuclear medicine examinations.";
                case "contrastVolumeMl" -> "This examination uses contrast: enter the volume administered.";
                default -> "This field is mandatory or not valid.";
            });
        }
        return errors;
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
