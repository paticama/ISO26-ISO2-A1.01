package com.sescam.radiodiagnostics.client.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sescam.radiodiagnostics.common.integration.ConnectionCheckResultDTO;
import com.sescam.radiodiagnostics.common.integration.IntegrationSettingsDTO;
import com.sescam.radiodiagnostics.common.integration.IntegrationSummaryDTO;
import com.sescam.radiodiagnostics.common.integration.SettingsCheckResultDTO;
import com.sescam.radiodiagnostics.common.integration.UpdateIntegrationResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * REST client of the UC-ADM-04 server API ({@code /api/v1/integrations}), using the DTOs of sescam-common.
 * The credential is only sent in request bodies; error messages shown to the user come from the server and
 * never include it.
 */
@Component
public class IntegrationApiClient {

    static final String PATH = "/api/v1/integrations";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public IntegrationApiClient(RestClient.Builder builder, ObjectMapper objectMapper,
                                @Value("${sescam.server.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public List<IntegrationSummaryDTO> list(String actorId, String actorRole) {
        return call(() -> restClient.get().uri(PATH)
                .headers(h -> actor(h, actorId, actorRole))
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(new ParameterizedTypeReference<List<IntegrationSummaryDTO>>() {
                }));
    }

    public IntegrationSummaryDTO get(String id, String actorId, String actorRole) {
        return call(() -> restClient.get().uri(PATH + "/{id}", id)
                .headers(h -> actor(h, actorId, actorRole))
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(IntegrationSummaryDTO.class));
    }

    /** Step 5: validate the settings and check the connection; nothing is saved. */
    public SettingsCheckResultDTO validate(String id, IntegrationSettingsDTO settings, String actorId,
                                           String actorRole) {
        return call(() -> restClient.post().uri(PATH + "/{id}/validate", id)
                .headers(h -> actor(h, actorId, actorRole))
                .contentType(MediaType.APPLICATION_JSON).body(settings)
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(SettingsCheckResultDTO.class));
    }

    /** Steps 6-7: confirm and save. */
    public UpdateIntegrationResponseDTO update(String id, IntegrationSettingsDTO settings, long expectedVersion,
                                               String actorId, String actorRole) {
        return call(() -> restClient.put().uri(PATH + "/{id}", id)
                .headers(h -> actor(h, actorId, actorRole))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("settings", settings, "expectedVersion", expectedVersion))
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(UpdateIntegrationResponseDTO.class));
    }

    /** A3: retry or diagnostic review of the saved configuration. */
    public ConnectionCheckResultDTO check(String id, String actorId, String actorRole) {
        return call(() -> restClient.post().uri(PATH + "/{id}/check", id)
                .headers(h -> actor(h, actorId, actorRole))
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(ConnectionCheckResultDTO.class));
    }

    private static void actor(org.springframework.http.HttpHeaders headers, String actorId, String actorRole) {
        headers.set("X-Actor-Id", actorId == null ? "" : actorId);
        headers.set("X-Actor-Role", actorRole == null ? "" : actorRole);
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException ex) {
            throw new IntegrationClientException(IntegrationClientException.Kind.SERVER_UNREACHABLE,
                    "The server cannot be reached. Please retry in a moment.", List.of(), null, ex);
        }
    }

    IntegrationClientException toException(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        JsonNode body = readBody(response);
        String message = text(body, "message");
        List<String> errors = new ArrayList<>();
        if (body != null && body.has("errors") && body.get("errors").isArray()) {
            body.get("errors").forEach(e -> errors.add(e.asText()));
        }
        IntegrationClientException.Kind kind = switch (status) {
            case 401, 403 -> IntegrationClientException.Kind.FORBIDDEN;
            case 404 -> IntegrationClientException.Kind.NOT_FOUND;
            case 400 -> IntegrationClientException.Kind.INVALID_SETTINGS;
            case 409 -> IntegrationClientException.Kind.CONFLICT;
            case 502 -> IntegrationClientException.Kind.CONNECTION_FAILED;
            default -> IntegrationClientException.Kind.SAVE_FAILED;
        };
        if (message == null) {
            message = switch (kind) {
                case FORBIDDEN -> "You are not authorized to manage integrations.";
                case NOT_FOUND -> "The integration does not exist.";
                case INVALID_SETTINGS -> "The settings are not valid.";
                case CONFLICT -> "Someone else changed this integration. Reload and try again.";
                case CONNECTION_FAILED -> "The connection check failed.";
                default -> "The request could not be completed. Please retry.";
            };
        }
        return new IntegrationClientException(kind, message, errors, text(body, "status"), null);
    }

    private JsonNode readBody(ClientHttpResponse response) {
        try {
            String raw = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
            return raw.isBlank() ? null : objectMapper.readTree(raw);
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
