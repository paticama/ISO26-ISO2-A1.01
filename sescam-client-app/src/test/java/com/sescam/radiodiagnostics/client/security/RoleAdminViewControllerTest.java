package com.sescam.radiodiagnostics.client.security;

import es.uclm.esi.sescam.security.api.ChangeOutcome;
import es.uclm.esi.sescam.security.api.RoleAssignmentDTO;
import es.uclm.esi.sescam.security.api.RoleChangeRequestDTO;
import es.uclm.esi.sescam.security.api.RoleChangeResultDTO;
import es.uclm.esi.sescam.security.api.RoleDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(RoleAdminViewController.class)
class RoleAdminViewControllerTest {

    private static final RoleChangeRequestDTO REQUEST = new RoleChangeRequestDTO("admin-1", "user-7", "PHYSICIAN");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleAdminApiClient apiClient;

    private static RoleChangeResultDTO result(ChangeOutcome outcome, String message, String... roles) {
        return new RoleChangeResultDTO(outcome, message,
                java.util.Arrays.stream(roles).map(RoleDTO::new).toList(), List.of("VIEW_PATIENTS"));
    }

    @Test
    void opensTheAreaWithAnEmptyForm() throws Exception {
        mockMvc.perform(get("/security/roles"))
                .andExpect(status().isOk())
                .andExpect(view().name("security/roles"))
                .andExpect(content().string(containsString("User roles and access control")));
    }

    @Test
    void lookupShowsCurrentRolesAndAccountStatus() throws Exception {
        when(apiClient.getRoleAssignments("user-7", "admin-1")).thenReturn(
                new RoleAssignmentDTO("user-7", "ACTIVE", List.of(new RoleDTO("NURSE"))));

        mockMvc.perform(get("/security/roles/lookup").param("userId", "user-7").header("X-Actor-Id", "admin-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Current roles of user-7")))
                .andExpect(content().string(containsString("ACTIVE")))
                .andExpect(content().string(containsString("NURSE")));
    }

    @Test
    void lookupFailuresAreShownAndNothingIsEditable() throws Exception {
        when(apiClient.getRoleAssignments("ghost", "admin-1")).thenThrow(
                new RoleAdminClientException(RoleAdminClientException.Kind.USER_NOT_FOUND, "User ghost not found"));

        mockMvc.perform(get("/security/roles/lookup").param("userId", "ghost").header("X-Actor-Id", "admin-1"))
                .andExpect(content().string(containsString("User ghost not found")))
                .andExpect(content().string(not(containsString("id=\"change-form\""))));
    }

    @Test
    void unauthorizedLookupIsShown() throws Exception {
        when(apiClient.getRoleAssignments("user-7", "patient-1")).thenThrow(
                new RoleAdminClientException(RoleAdminClientException.Kind.FORBIDDEN, "Not allowed to manage roles."));

        mockMvc.perform(get("/security/roles/lookup").param("userId", "user-7").header("X-Actor-Id", "patient-1"))
                .andExpect(content().string(containsString("Not allowed to manage roles.")));
    }

    @Test
    void invalidFormIsNotSentToTheServer() throws Exception {
        mockMvc.perform(get("/security/roles/lookup").param("userId", " "))
                .andExpect(content().string(containsString("Enter the user account to look up.")));
        mockMvc.perform(post("/security/roles/validate").param("userId", "user-7").param("roleName", ""))
                .andExpect(content().string(containsString("Enter the role to assign.")));
        mockMvc.perform(post("/security/roles/confirm").param("userId", "user-7").param("roleName", "R;1"))
                .andExpect(content().string(containsString("Use only letters")));

        verifyNoInteractions(apiClient);
    }

    @Test
    void validChangeShowsTheResultingAccessAndAsksForConfirmation() throws Exception {
        when(apiClient.validateRoleChange(REQUEST))
                .thenReturn(result(ChangeOutcome.VALID, "The change is valid.", "PHYSICIAN"));

        mockMvc.perform(post("/security/roles/validate").param("userId", "user-7").param("roleName", "PHYSICIAN")
                        .header("X-Actor-Id", "admin-1"))
                .andExpect(content().string(containsString("id=\"outcome-valid\"")))
                .andExpect(content().string(containsString("VIEW_PATIENTS")))
                .andExpect(content().string(containsString("Confirm change")));

        verify(apiClient, never()).confirmRoleChange(any());
    }

    @Test
    void confirmedChangeIsReportedWithTheEffectiveResult() throws Exception {
        when(apiClient.confirmRoleChange(REQUEST))
                .thenReturn(result(ChangeOutcome.CONFIRMED, "Role saved.", "PHYSICIAN"));

        mockMvc.perform(post("/security/roles/confirm").param("userId", "user-7").param("roleName", "PHYSICIAN")
                        .header("X-Actor-Id", "admin-1"))
                .andExpect(content().string(containsString("id=\"outcome-confirmed\"")))
                .andExpect(content().string(containsString("Role saved.")))
                .andExpect(content().string(containsString("VIEW_PATIENTS")));
    }

    @Test
    void refusedChangesExplainWhyAndKeepTheAssignment() throws Exception {
        when(apiClient.validateRoleChange(REQUEST)).thenReturn(result(ChangeOutcome.INVALID, "Unknown role.", "NURSE"));
        mockMvc.perform(post("/security/roles/validate").param("userId", "user-7").param("roleName", "PHYSICIAN")
                        .header("X-Actor-Id", "admin-1"))
                .andExpect(content().string(containsString("id=\"outcome-invalid\"")))
                .andExpect(content().string(containsString("Unknown role.")))
                .andExpect(content().string(containsString("has not been changed")))
                .andExpect(content().string(not(containsString("Confirm change"))));

        when(apiClient.validateRoleChange(REQUEST))
                .thenReturn(result(ChangeOutcome.SAFEGUARD_REQUIRED, "Safeguards required."));
        mockMvc.perform(post("/security/roles/validate").param("userId", "user-7").param("roleName", "PHYSICIAN")
                        .header("X-Actor-Id", "admin-1"))
                .andExpect(content().string(containsString("id=\"outcome-safeguard\"")))
                .andExpect(content().string(not(containsString("Confirm change"))));

        when(apiClient.validateRoleChange(REQUEST)).thenReturn(result(ChangeOutcome.UNAUTHORIZED, "Not authorized."));
        mockMvc.perform(post("/security/roles/validate").param("userId", "user-7").param("roleName", "PHYSICIAN")
                        .header("X-Actor-Id", "admin-1"))
                .andExpect(content().string(containsString("id=\"outcome-unauthorized\"")));
    }

    @Test
    void saveFailureSaysAccessDidNotChangeAndOffersRetry() throws Exception {
        when(apiClient.confirmRoleChange(REQUEST)).thenReturn(result(ChangeOutcome.SAVE_FAILED, "Could not save."));

        mockMvc.perform(post("/security/roles/confirm").param("userId", "user-7").param("roleName", "PHYSICIAN")
                        .header("X-Actor-Id", "admin-1"))
                .andExpect(content().string(containsString("id=\"outcome-save-failed\"")))
                .andExpect(content().string(containsString("Access has NOT changed.")))
                .andExpect(content().string(containsString("Retry")));
    }

    @Test
    void unreachableServerOnChangeIsShownAsFailure() throws Exception {
        when(apiClient.confirmRoleChange(REQUEST)).thenThrow(new RoleAdminClientException(
                RoleAdminClientException.Kind.SERVER_UNREACHABLE, "The server cannot be reached."));

        mockMvc.perform(post("/security/roles/confirm").param("userId", "user-7").param("roleName", "PHYSICIAN")
                        .header("X-Actor-Id", "admin-1"))
                .andExpect(content().string(containsString("The server cannot be reached.")))
                .andExpect(content().string(containsString("no assignment was changed")));
    }
}
