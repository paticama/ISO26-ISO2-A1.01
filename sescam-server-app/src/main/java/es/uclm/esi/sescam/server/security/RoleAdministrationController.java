package es.uclm.esi.sescam.server.security;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.uclm.esi.sescam.security.api.AccessControlService;
import es.uclm.esi.sescam.security.api.AccessDeniedException;
import es.uclm.esi.sescam.security.api.RoleAssignmentDTO;
import es.uclm.esi.sescam.security.api.RoleChangeRequestDTO;
import es.uclm.esi.sescam.security.api.RoleChangeResultDTO;

/** REST API of UC-ADM-02. Talks only to {@link AccessControlService}, never to its implementation. */
@RestController
@RequestMapping("/api/security")
public class RoleAdministrationController {

    private final AccessControlService accessControlService;

    public RoleAdministrationController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    /** Steps 1-2: current role assignments and account status of a user. */
    @GetMapping("/users/{userId}/roles")
    public RoleAssignmentDTO getRoleAssignments(@PathVariable("userId") String userId,
                                                @RequestParam("administratorId") String administratorId) {
        return accessControlService.getRoleAssignments(administratorId, userId);
    }

    /** Steps 3-4: validate a role change without saving it. */
    @PostMapping("/users/{userId}/roles/validate")
    public ResponseEntity<?> validateRoleChange(@PathVariable("userId") String userId,
                                                @RequestBody RoleChangeRequestDTO request) {
        if (!userId.equals(request.targetUserId())) {
            return pathMismatch();
        }
        return respond(accessControlService.validateRoleChange(request));
    }

    /** Steps 5-6: save the confirmed role change. */
    @PostMapping("/users/{userId}/roles/confirm")
    public ResponseEntity<?> confirmRoleChange(@PathVariable("userId") String userId,
                                               @RequestBody RoleChangeRequestDTO request) {
        if (!userId.equals(request.targetUserId())) {
            return pathMismatch();
        }
        return respond(accessControlService.confirmRoleChange(request));
    }

    private static ResponseEntity<?> pathMismatch() {
        return ResponseEntity.badRequest().body(Map.of("message", "targetUserId does not match the path"));
    }

    private static ResponseEntity<RoleChangeResultDTO> respond(RoleChangeResultDTO result) {
        HttpStatus status = switch (result.outcome()) {
            case VALID, CONFIRMED -> HttpStatus.OK;
            case UNAUTHORIZED -> HttpStatus.FORBIDDEN;
            case INVALID -> HttpStatus.UNPROCESSABLE_ENTITY;
            case SAFEGUARD_REQUIRED -> HttpStatus.CONFLICT;
            case SAVE_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(result);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, String>> accessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Map<String, String>> notFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }
}
