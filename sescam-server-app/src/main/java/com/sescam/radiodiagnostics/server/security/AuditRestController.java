package com.sescam.radiodiagnostics.server.security;

import com.sescam.radiodiagnostics.common.security.AuditQueryService;
import com.sescam.radiodiagnostics.common.security.AuditSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.security.AuditSearchResultDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * REST API of UC-ADM-03 – Audit and compliance management. Read only: there is no POST, PUT or DELETE.
 */
@RestController
@RequestMapping("/api/v1/audit/events")
public class AuditRestController {

    private final AuditQueryService auditQueryService;

    public AuditRestController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    public ResponseEntity<AuditSearchResultDTO> searchEvents(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(value = "performedBy", required = false) String performedBy,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "resourceKey", required = false) String resourceKey,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        AuditSearchCriteriaDTO criteria = new AuditSearchCriteriaDTO(from, to, performedBy, action, resourceKey);
        return ResponseEntity.ok(auditQueryService.searchEvents(criteria, actorId, actorRole));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> exportEvents(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(value = "performedBy", required = false) String performedBy,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "resourceKey", required = false) String resourceKey,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        AuditSearchCriteriaDTO criteria = new AuditSearchCriteriaDTO(from, to, performedBy, action, resourceKey);
        String csv = auditQueryService.exportEventsAsCsv(criteria, actorId, actorRole);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-events.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
