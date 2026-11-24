package com.sescam.radiodiagnostics.client.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.uclm.esi.sescam.security.api.ChangeOutcome;
import es.uclm.esi.sescam.security.api.RoleAssignmentDTO;
import es.uclm.esi.sescam.security.api.RoleChangeRequestDTO;
import es.uclm.esi.sescam.security.api.RoleChangeResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RoleAdminApiClientTest {

    private static final String BASE = "http://server";
    private static final RoleChangeRequestDTO REQUEST = new RoleChangeRequestDTO("admin-1", "user-7", "PHYSICIAN");

    private MockRestServiceServer server;
    private RoleAdminApiClient client;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder()
                .messageConverters(c -> c.add(0, new MappingJackson2HttpMessageConverter(mapper)));
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RoleAdminApiClient(builder, mapper, BASE);
    }

    private static String result(String outcome, String message, String roles) {
        return "{\"outcome\":\"" + outcome + "\",\"message\":\"" + message + "\",\"roles\":" + roles
                + ",\"effectivePermissions\":[\"VIEW_PATIENTS\"]}";
    }

    @Test
    void getRoleAssignmentsSendsTheAdministratorAndReadsTheAssignment() {
        server.expect(requestTo(BASE + "/api/security/users/user-7/roles?administratorId=admin-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"userId\":\"user-7\",\"accountStatus\":\"ACTIVE\",\"roles\":[{\"name\":\"NURSE\"}]}",
                        MediaType.APPLICATION_JSON));

        RoleAssignmentDTO assignment = client.getRoleAssignments("user-7", "admin-1");

        assertThat(assignment.accountStatus()).isEqualTo("ACTIVE");
        assertThat(assignment.roles()).singleElement().satisfies(r -> assertThat(r.name()).isEqualTo("NURSE"));
        server.verify();
    }

    @Test
    void validateAndConfirmPostTheChangeAndReturnTheResult() {
        server.expect(requestTo(BASE + "/api/security/users/user-7/roles/validate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.administratorId").value("admin-1"))
                .andExpect(jsonPath("$.targetUserId").value("user-7"))
                .andExpect(jsonPath("$.roleName").value("PHYSICIAN"))
                .andRespond(withSuccess(result("VALID", "Change is valid.", "[{\"name\":\"PHYSICIAN\"}]"),
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/security/users/user-7/roles/confirm"))
                .andRespond(withSuccess(result("CONFIRMED", "Saved.", "[{\"name\":\"PHYSICIAN\"}]"),
                        MediaType.APPLICATION_JSON));

        RoleChangeResultDTO valid = client.validateRoleChange(REQUEST);
        RoleChangeResultDTO confirmed = client.confirmRoleChange(REQUEST);

        assertThat(valid.outcome()).isEqualTo(ChangeOutcome.VALID);
        assertThat(confirmed.outcome()).isEqualTo(ChangeOutcome.CONFIRMED);
        assertThat(confirmed.effectivePermissions()).containsExactly("VIEW_PATIENTS");
        server.verify();
    }

    @Test
    void refusedChangesAreReturnedAsResultsNotThrown() {
        // A1 403, A2 422, A4 409, A3 500: the server body is a RoleChangeResultDTO in all four
        String[][] cases = {
                {"403", "UNAUTHORIZED"}, {"422", "INVALID"}, {"409", "SAFEGUARD_REQUIRED"}, {"500", "SAVE_FAILED"}};
        for (String[] c : cases) {
            server.reset();
            server.expect(requestTo(BASE + "/api/security/users/user-7/roles/confirm"))
                    .andRespond(withStatus(HttpStatus.valueOf(Integer.parseInt(c[0])))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(result(c[1], "msg", "[]")));

            assertThat(client.confirmRoleChange(REQUEST).outcome()).isEqualTo(ChangeOutcome.valueOf(c[1]));
        }
    }

    @Test
    void lookupErrorsAreTranslatedWithTheServerMessage() {
        server.expect(requestTo(BASE + "/api/security/users/ghost/roles?administratorId=admin-1"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"User ghost not found\"}"));
        assertThatThrownBy(() -> client.getRoleAssignments("ghost", "admin-1"))
                .isInstanceOfSatisfying(RoleAdminClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(RoleAdminClientException.Kind.USER_NOT_FOUND);
                    assertThat(ex.getMessage()).isEqualTo("User ghost not found");
                });

        server.reset();
        server.expect(requestTo(BASE + "/api/security/users/user-7/roles?administratorId=p-1"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).body("not json"));
        assertThatThrownBy(() -> client.getRoleAssignments("user-7", "p-1"))
                .isInstanceOfSatisfying(RoleAdminClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(RoleAdminClientException.Kind.FORBIDDEN);
                    assertThat(ex.getMessage()).contains("not authorized");
                    assertThat(ex.canRetry()).isFalse();
                });
    }

    @Test
    void pathMismatchAndUnknownUserOnPostAreExceptions() {
        server.expect(requestTo(BASE + "/api/security/users/user-7/roles/validate"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"targetUserId does not match the path\"}"));
        assertThatThrownBy(() -> client.validateRoleChange(REQUEST))
                .isInstanceOfSatisfying(RoleAdminClientException.class,
                        ex -> assertThat(ex.getKind()).isEqualTo(RoleAdminClientException.Kind.INVALID_REQUEST));

        server.reset();
        server.expect(requestTo(BASE + "/api/security/users/user-7/roles/validate"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> client.validateRoleChange(REQUEST))
                .isInstanceOfSatisfying(RoleAdminClientException.class,
                        ex -> assertThat(ex.getKind()).isEqualTo(RoleAdminClientException.Kind.USER_NOT_FOUND));
    }

    @Test
    void unreadableAnswerAndUnreachableServerSayNothingWasChanged() {
        server.expect(requestTo(BASE + "/api/security/users/user-7/roles/confirm"))
                .andRespond(withSuccess("garbage", MediaType.TEXT_PLAIN));
        assertThatThrownBy(() -> client.confirmRoleChange(REQUEST))
                .isInstanceOfSatisfying(RoleAdminClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(RoleAdminClientException.Kind.FAILED);
                    assertThat(ex.getMessage()).contains("not been changed");
                    assertThat(ex.canRetry()).isTrue();
                });

        server.reset();
        server.expect(requestTo(BASE + "/api/security/users/user-7/roles/confirm"))
                .andRespond(withException(new java.net.ConnectException("refused")));
        assertThatThrownBy(() -> client.confirmRoleChange(REQUEST))
                .isInstanceOfSatisfying(RoleAdminClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(RoleAdminClientException.Kind.SERVER_UNREACHABLE);
                    assertThat(ex.getMessage()).contains("has not been changed");
                    assertThat(ex).hasCauseInstanceOf(ResourceAccessException.class);
                });
    }
}
