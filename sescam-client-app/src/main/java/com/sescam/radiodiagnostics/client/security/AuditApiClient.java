package com.sescam.radiodiagnostics.client.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sescam.radiodiagnostics.common.security.AuditSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.security.AuditSearchResultDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * REST client of the UC-ADM-03 server API ({@code /api/v1/audit/events}), using the DTOs of sescam-common.
 */
@Component
public class AuditApiClient {

    static final String EVENTS_PATH = "/api/v1/audit/events";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AuditApiClient(RestClient.Builder builder, ObjectMapper objectMapper,
                          @Value("${sescam.server.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public AuditSearchResultDTO search(AuditSearchCriteriaDTO criteria, String actorId, String actorRole) {
        return call(() -> restClient.get()
                .uri(b -> buildUri(b, EVENTS_PATH, criteria))
                .header("X-Actor-Id", nullToEmpty(actorId))
                .header("X-Actor-Role", nullToEmpty(actorRole))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(AuditSearchResultDTO.class));
    }

    public String exportCsv(AuditSearchCriteriaDTO criteria, String actorId, String actorRole) {
        return call(() -> restClient.get()
                .uri(b -> buildUri(b, EVENTS_PATH + "/export", criteria))
                .header("X-Actor-Id", nullToEmpty(actorId))
                .header("X-Actor-Role", nullToEmpty(actorRole))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(String.class));
    }

    private static <T> T call(java.util.function.Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException ex) {
            throw new AuditClientException(AuditClientException.Kind.SERVER_UNREACHABLE,
                    "The server cannot be reached. Please retry in a moment.", ex);
        }
    }

    private static URI buildUri(UriBuilder builder, String path, AuditSearchCriteriaDTO c) {
        builder.path(path)
                .queryParam("from", c.from())
                .queryParam("to", c.to());
        if (c.performedBy() != null) {
            builder.queryParam("performedBy", c.performedBy());
        }
        if (c.action() != null) {
            builder.queryParam("action", c.action());
        }
        if (c.resourceKey() != null) {
            builder.queryParam("resourceKey", c.resourceKey());
        }
        return builder.build();
    }

    AuditClientException toException(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String message = readMessage(response);
        AuditClientException.Kind kind = switch (status) {
            case 401, 403 -> AuditClientException.Kind.FORBIDDEN;
            case 400 -> AuditClientException.Kind.INVALID_QUERY;
            case 422 -> AuditClientException.Kind.QUERY_TOO_BROAD;
            default -> AuditClientException.Kind.QUERY_FAILED;
        };
        if (message == null) {
            message = switch (kind) {
                case FORBIDDEN -> "You are not authorized to view audit records.";
                case INVALID_QUERY -> "The search is not valid. Check the filters.";
                case QUERY_TOO_BROAD -> "The search is too broad. Narrow the time range or add filters.";
                default -> "The audit records could not be queried. Please retry.";
            };
        }
        return new AuditClientException(kind, message);
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

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
