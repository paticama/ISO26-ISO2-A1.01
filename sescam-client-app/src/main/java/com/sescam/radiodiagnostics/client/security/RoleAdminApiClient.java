package com.sescam.radiodiagnostics.client.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.uclm.esi.sescam.security.api.RoleAssignmentDTO;
import es.uclm.esi.sescam.security.api.RoleChangeRequestDTO;
import es.uclm.esi.sescam.security.api.RoleChangeResultDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * REST client of the UC-ADM-02 server API ({@code /api/security/users/{id}/roles}), using the DTOs of sescam-common.
 *
 * <p>The server answers a role change with a {@link RoleChangeResultDTO} even when the change is refused
 * (403 unauthorized, 422 invalid, 409 safeguard required, 500 save failed), so those responses are returned as
 * results and not thrown: the outcome tells the view what to show. Everything else is a
 * {@link RoleAdminClientException}.
 */
@Component
public class RoleAdminApiClient {

    static final String USERS_PATH = "/api/security/users/{userId}/roles";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public RoleAdminApiClient(RestClient.Builder builder, ObjectMapper objectMapper,
                              @Value("${sescam.server.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    /** Steps 1-2: current role assignments and account status of a user. */
    public RoleAssignmentDTO getRoleAssignments(String userId, String administratorId) {
        return call(() -> restClient.get()
                .uri(b -> b.path(USERS_PATH).queryParam("administratorId", administratorId).build(userId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(RoleAssignmentDTO.class));
    }

    /** Steps 3-4: validate a role change without saving it. */
    public RoleChangeResultDTO validateRoleChange(RoleChangeRequestDTO request) {
        return post(USERS_PATH + "/validate", request);
    }

    /** Steps 5-6: save the confirmed role change. */
    public RoleChangeResultDTO confirmRoleChange(RoleChangeRequestDTO request) {
        return post(USERS_PATH + "/confirm", request);
    }

    private RoleChangeResultDTO post(String path, RoleChangeRequestDTO request) {
        return call(() -> restClient.post()
                .uri(path, request.targetUserId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange((req, res) -> {
                    int status = res.getStatusCode().value();
                    if (status == 400 || status == 404) {
                        throw toException(res);
                    }
                    String body = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    try {
                        return objectMapper.readValue(body, RoleChangeResultDTO.class);
                    } catch (IOException | RuntimeException ex) {
                        throw new RoleAdminClientException(RoleAdminClientException.Kind.FAILED,
                                "The server answer could not be read. The assignment has not been changed.", ex);
                    }
                }));
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException ex) {
            throw new RoleAdminClientException(RoleAdminClientException.Kind.SERVER_UNREACHABLE,
                    "The server cannot be reached. The assignment has not been changed. Please retry in a moment.", ex);
        }
    }

    RoleAdminClientException toException(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String message = readMessage(response);
        RoleAdminClientException.Kind kind = switch (status) {
            case 401, 403 -> RoleAdminClientException.Kind.FORBIDDEN;
            case 404 -> RoleAdminClientException.Kind.USER_NOT_FOUND;
            case 400 -> RoleAdminClientException.Kind.INVALID_REQUEST;
            default -> RoleAdminClientException.Kind.FAILED;
        };
        if (message == null) {
            message = switch (kind) {
                case FORBIDDEN -> "You are not authorized to manage user roles.";
                case USER_NOT_FOUND -> "The user account does not exist.";
                case INVALID_REQUEST -> "The request is not valid.";
                default -> "The request could not be completed. Please retry.";
            };
        }
        return new RoleAdminClientException(kind, message);
    }

    private String readMessage(ClientHttpResponse response) {
        try {
            String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(body);
            JsonNode message = node == null ? null : node.get("message");
            return message == null || message.isNull() ? null : message.asText();
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }
}
