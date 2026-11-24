package com.sescam.radiodiagnostics.client.diagnostics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionRecordDTO;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ScheduledProcedureDTO;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * REST client of the UC-PHY-05 server API ({@code /api/v1/executions}), using the DTOs of sescam-common.
 */
@Component
public class ExecutionApiClient {

    static final String EXECUTIONS_PATH = "/api/v1/executions";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ExecutionApiClient(RestClient.Builder builder, ObjectMapper objectMapper,
                              @Value("${sescam.server.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    /** Step 1: booked procedures awaiting execution. */
    public List<ScheduledProcedureDTO> listPending(String actorId, String actorRole) {
        return call(() -> restClient.get()
                .uri(EXECUTIONS_PATH + "/pending")
                .header("X-Actor-Id", nullToEmpty(actorId))
                .header("X-Actor-Role", nullToEmpty(actorRole))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(new ParameterizedTypeReference<List<ScheduledProcedureDTO>>() { }));
    }

    /** Steps 3-5 with incidents (2a) and abort (2b). */
    public ExecutionRecordDTO recordExecution(ExecutionPayload payload, String actorId, String actorRole) {
        return call(() -> restClient.post()
                .uri(EXECUTIONS_PATH)
                .header("X-Actor-Id", nullToEmpty(actorId))
                .header("X-Actor-Role", nullToEmpty(actorRole))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(ExecutionRecordDTO.class));
    }

    /** Request body of the server API; the actor travels in the headers. */
    public record ExecutionPayload(
            String orderId,
            Instant startedAt,
            Instant endedAt,
            String roomEquipmentId,
            String contrastBatch,
            Double contrastVolumeMl,
            Double doseDlp,
            Double doseCtdiVol,
            Double radiotracerActivityMBq,
            boolean informedConsentVerified,
            boolean aborted,
            String abortReason,
            List<IncidentDTO> incidents
    ) {
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException ex) {
            throw new ExecutionClientException(ExecutionClientException.Kind.SERVER_UNREACHABLE,
                    "The server cannot be reached. Nothing has been recorded. Please retry in a moment.", List.of(), ex);
        }
    }

    ExecutionClientException toException(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        JsonNode body = readBody(response);
        JsonNode messageNode = body == null ? null : body.get("message");
        String message = messageNode == null || messageNode.isNull() ? null : messageNode.asText();
        ExecutionClientException.Kind kind = switch (status) {
            case 401, 403 -> ExecutionClientException.Kind.FORBIDDEN;
            case 400 -> ExecutionClientException.Kind.INVALID_DATA;
            case 404 -> ExecutionClientException.Kind.PROCEDURE_NOT_FOUND;
            case 409 -> ExecutionClientException.Kind.ALREADY_RECORDED;
            default -> ExecutionClientException.Kind.FAILED;
        };
        if (message == null) {
            message = switch (kind) {
                case FORBIDDEN -> "You are not authorized to log procedure executions.";
                case INVALID_DATA -> "The execution log is not valid. Check the highlighted fields.";
                case PROCEDURE_NOT_FOUND -> "The selected procedure is not booked.";
                case ALREADY_RECORDED -> "The execution of this procedure is already recorded.";
                default -> "The execution could not be recorded. Please retry.";
            };
        }
        List<String> invalidFields = new ArrayList<>();
        JsonNode fields = body == null ? null : body.get("invalidFields");
        if (kind == ExecutionClientException.Kind.INVALID_DATA && fields != null && fields.isArray()) {
            fields.forEach(f -> invalidFields.add(f.asText()));
        }
        return new ExecutionClientException(kind, message, invalidFields, null);
    }

    private JsonNode readBody(ClientHttpResponse response) {
        try {
            return objectMapper.readTree(new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
