package com.sescam.radiodiagnostics.server.catalogue;

import com.sescam.radiodiagnostics.common.catalogue.CatalogueService;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationParameterDTO;
import com.sescam.radiodiagnostics.common.catalogue.UpdateParameterRequestDTO;
import com.sescam.radiodiagnostics.common.catalogue.UpdateParameterResponseDTO;
import com.sescam.radiodiagnostics.common.catalogue.ValidationResultDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/configurations")
public class ConfigurationRestController {

    private final CatalogueService catalogueService;

    public ConfigurationRestController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    @GetMapping
    public ResponseEntity<List<ConfigurationParameterDTO>> listParameters(
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.ok(catalogueService.getAllParameters(actorId, actorRole));
    }

    @GetMapping("/{key}")
    public ResponseEntity<ConfigurationParameterDTO> getParameter(
            @PathVariable("key") String key,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.ok(catalogueService.getParameter(key, actorId, actorRole));
    }

    @PostMapping("/{key}/validate")
    public ResponseEntity<ValidationResultDTO> validateParameter(
            @PathVariable("key") String key,
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        String proposedValue = payload != null ? payload.get("proposedValue") : null;
        return ResponseEntity.ok(catalogueService.validateParameterUpdate(key, proposedValue, actorId, actorRole));
    }

    @PutMapping("/{key}")
    public ResponseEntity<UpdateParameterResponseDTO> updateParameter(
            @PathVariable("key") String key,
            @RequestBody UpdateValuePayload payload,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        UpdateParameterRequestDTO request = new UpdateParameterRequestDTO(
                key, payload.proposedValue(), payload.expectedVersion(), actorId, actorRole
        );
        return ResponseEntity.ok(catalogueService.updateParameter(request));
    }

    public record UpdateValuePayload(String proposedValue, long expectedVersion) {
    }
}
