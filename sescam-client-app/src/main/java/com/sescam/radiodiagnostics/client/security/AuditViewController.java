package com.sescam.radiodiagnostics.client.security;

import com.sescam.radiodiagnostics.common.security.AuditSearchResultDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.ZoneId;
import java.util.Map;

/**
 * View of UC-ADM-03 in the client app: audit and compliance area for administrators and compliance reviewers.
 * <p>
 * The actor identity comes from the {@code X-Actor-Id}/{@code X-Actor-Role} headers until the login of
 * UC-ADM-02 is available in the client.
 */
@Controller
@RequestMapping("/security/audit")
public class AuditViewController {

    static final String VIEW = "security/audit";

    private final AuditApiClient apiClient;
    private final ZoneId zone;

    public AuditViewController(AuditApiClient apiClient, @Value("${sescam.client.zone}") String zone) {
        this.apiClient = apiClient;
        this.zone = ZoneId.of(zone);
    }

    /** Step 1: open the audit area with an empty form. */
    @GetMapping
    public String openAuditArea(Model model) {
        model.addAttribute("form", new AuditFilterForm());
        model.addAttribute("zone", zone);
        return VIEW;
    }

    /** Steps 2–4: validate the filters, search, and show the time-ordered events. */
    @GetMapping("/search")
    public String search(@ModelAttribute("form") AuditFilterForm form, Model model,
                         @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                         @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        model.addAttribute("zone", zone);
        Map<String, String> errors = form.validate();
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            return VIEW;
        }
        try {
            AuditSearchResultDTO result = apiClient.search(form.toCriteria(zone), actorId, actorRole);
            model.addAttribute("result", result);
        } catch (AuditClientException ex) {
            model.addAttribute("failure", ex);
        }
        return VIEW;
    }

    /** Step 5: export the same search as a CSV file. */
    @GetMapping("/export")
    public ResponseEntity<String> export(@ModelAttribute("form") AuditFilterForm form,
                                         @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                                         @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        Map<String, String> errors = form.validate();
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN)
                    .body(String.join("\n", errors.values()));
        }
        try {
            String csv = apiClient.exportCsv(form.toCriteria(zone), actorId, actorRole);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-events.csv\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv);
        } catch (AuditClientException ex) {
            HttpStatus status = ex.getKind() == AuditClientException.Kind.FORBIDDEN
                    ? HttpStatus.FORBIDDEN : ex.canRefine() ? HttpStatus.BAD_REQUEST : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(status).contentType(MediaType.TEXT_PLAIN).body(ex.getMessage());
        }
    }
}
