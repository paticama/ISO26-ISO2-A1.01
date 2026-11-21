package es.uclm.esi.sescam.server.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import es.uclm.esi.sescam.security.api.AccessControlService;
import es.uclm.esi.sescam.security.api.AccessDeniedException;
import es.uclm.esi.sescam.security.api.ChangeOutcome;
import es.uclm.esi.sescam.security.api.RoleAssignmentDTO;
import es.uclm.esi.sescam.security.api.RoleChangeRequestDTO;
import es.uclm.esi.sescam.security.api.RoleChangeResultDTO;
import es.uclm.esi.sescam.security.api.RoleDTO;

class RoleAdministrationControllerTest {

    private static final String BODY = "{\"administratorId\":\"admin\",\"targetUserId\":\"ana\",\"roleName\":\"Physician\"}";

    private AccessControlService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(AccessControlService.class);
        mvc = MockMvcBuilders.standaloneSetup(new RoleAdministrationController(service)).build();
    }

    private static RoleChangeResultDTO result(ChangeOutcome outcome) {
        return new RoleChangeResultDTO(outcome, "msg", List.of(new RoleDTO("Physician")), List.of("P"));
    }

    @Test
    void getReturnsTheCurrentAssignments() throws Exception {
        when(service.getRoleAssignments("admin", "ana"))
                .thenReturn(new RoleAssignmentDTO("ana", "ACTIVE", List.of(new RoleDTO("Patient"))));

        mvc.perform(get("/api/security/users/ana/roles").param("administratorId", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("ana"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0].name").value("Patient"));
    }

    @Test
    void getIsForbiddenWhenTheRequesterLacksThePermission() throws Exception {
        when(service.getRoleAssignments("ana", "pat")).thenThrow(new AccessDeniedException("no"));

        mvc.perform(get("/api/security/users/pat/roles").param("administratorId", "ana"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("no"));
    }

    @Test
    void getIsNotFoundForAnUnknownUser() throws Exception {
        when(service.getRoleAssignments("admin", "ghost")).thenThrow(new NoSuchElementException("Unknown user"));

        mvc.perform(get("/api/security/users/ghost/roles").param("administratorId", "admin"))
                .andExpect(status().isNotFound());
    }

    @Test
    void validateMapsEachOutcomeToItsStatus() throws Exception {
        Object[][] cases = {
            {ChangeOutcome.VALID, 200}, {ChangeOutcome.UNAUTHORIZED, 403}, {ChangeOutcome.INVALID, 422},
            {ChangeOutcome.SAFEGUARD_REQUIRED, 409}};
        for (Object[] c : cases) {
            when(service.validateRoleChange(any())).thenReturn(result((ChangeOutcome) c[0]));
            mvc.perform(post("/api/security/users/ana/roles/validate").contentType(MediaType.APPLICATION_JSON)
                            .content(BODY))
                    .andExpect(status().is((Integer) c[1]))
                    .andExpect(jsonPath("$.outcome").value(((ChangeOutcome) c[0]).name()));
        }
    }

    @Test
    void confirmMapsEachOutcomeToItsStatus() throws Exception {
        Object[][] cases = {
            {ChangeOutcome.CONFIRMED, 200}, {ChangeOutcome.UNAUTHORIZED, 403}, {ChangeOutcome.INVALID, 422},
            {ChangeOutcome.SAFEGUARD_REQUIRED, 409}, {ChangeOutcome.SAVE_FAILED, 500}};
        for (Object[] c : cases) {
            when(service.confirmRoleChange(any())).thenReturn(result((ChangeOutcome) c[0]));
            mvc.perform(post("/api/security/users/ana/roles/confirm").contentType(MediaType.APPLICATION_JSON)
                            .content(BODY))
                    .andExpect(status().is((Integer) c[1]));
        }
        verify(service, atLeastOnce()).confirmRoleChange(new RoleChangeRequestDTO("admin", "ana", "Physician"));
    }

    @Test
    void postRejectsAPathThatDoesNotMatchTheBody() throws Exception {
        mvc.perform(post("/api/security/users/other/roles/validate").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/security/users/other/roles/confirm").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
