package com.sescam.radiodiagnostics.server.integration;

import com.sescam.radiodiagnostics.common.integration.ConnectionCheckResultDTO;
import com.sescam.radiodiagnostics.common.integration.IntegrationService;
import com.sescam.radiodiagnostics.common.integration.IntegrationSettingsDTO;
import com.sescam.radiodiagnostics.common.integration.IntegrationSummaryDTO;
import com.sescam.radiodiagnostics.common.integration.SettingsCheckResultDTO;
import com.sescam.radiodiagnostics.common.integration.UpdateIntegrationRequestDTO;
import com.sescam.radiodiagnostics.common.integration.UpdateIntegrationResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API of UC-ADM-04 – Manage external integrations. The credential is only accepted in request bodies;
 * no response contains it.
 */
@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationRestController {

    private final IntegrationService integrationService;

    public IntegrationRestController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @GetMapping
    public ResponseEntity<List<IntegrationSummaryDTO>> list(
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.ok(integrationService.listIntegrations(actorId, actorRole));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IntegrationSummaryDTO> get(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.ok(integrationService.getIntegration(id, actorId, actorRole));
    }

    /** Step 5: validate and check the connection with the proposed settings; nothing is saved. */
    @PostMapping("/{id}/validate")
    public ResponseEntity<SettingsCheckResultDTO> validate(
            @PathVariable("id") String id,
            @RequestBody IntegrationSettingsDTO settings,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.ok(integrationService.validateSettings(id, settings, actorId, actorRole));
    }

    /** Steps 6-7: confirm and save. */
    @PutMapping("/{id}")
    public ResponseEntity<UpdateIntegrationResponseDTO> update(
            @PathVariable("id") String id,
            @RequestBody UpdatePayload payload,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        UpdateIntegrationRequestDTO request = new UpdateIntegrationRequestDTO(
                id, payload.settings(), payload.expectedVersion(), actorId, actorRole);
        return ResponseEntity.ok(integrationService.updateIntegration(request));
    }

    /** A3: retry or diagnostic review of the saved configuration. */
    @PostMapping("/{id}/check")
    public ResponseEntity<ConnectionCheckResultDTO> check(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.ok(integrationService.checkConnection(id, actorId, actorRole));
    }

    public record UpdatePayload(IntegrationSettingsDTO settings, long expectedVersion) {
    }
}
