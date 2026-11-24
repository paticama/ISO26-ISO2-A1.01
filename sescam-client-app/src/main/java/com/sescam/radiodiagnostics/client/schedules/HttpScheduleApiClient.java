package com.sescam.radiodiagnostics.client.schedules;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sescam.radiodiagnostics.common.schedules.AffectedAppointmentDTO;
import com.sescam.radiodiagnostics.common.schedules.OpenScheduleRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleChangeResultDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatusChangeRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleSummaryDTO;
import com.sescam.radiodiagnostics.common.schedules.UpdateScheduleRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * REST client of {@code /api/v1/schedules} (sescam-server-app).
 */
@Component
public class HttpScheduleApiClient implements ScheduleApiClient {

    static final String PATH = "/api/v1/schedules";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpScheduleApiClient(RestClient.Builder builder, ObjectMapper objectMapper,
                                 @Value("${sescam.server.base-url:http://localhost:8080}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ScheduleSummaryDTO> search(ScheduleSearchCriteriaDTO criteria, String actorId, String actorRole) {
        ScheduleSearchCriteriaDTO c = criteria == null ? ScheduleSearchCriteriaDTO.all() : criteria;
        return call(() -> restClient.get()
                .uri(b -> b.path(PATH)
                        .queryParamIfPresent("date", Optional.ofNullable(c.date()))
                        .queryParamIfPresent("resourceId", nonBlank(c.resourceId()))
                        .queryParamIfPresent("locationId", nonBlank(c.locationId()))
                        .queryParamIfPresent("status", Optional.ofNullable(c.status()))
                        .build())
                .headers(h -> actor(h, actorId, actorRole))
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(new ParameterizedTypeReference<List<ScheduleSummaryDTO>>() {}));
    }

    @Override
    public ScheduleDTO get(String scheduleId, String actorId, String actorRole) {
        return call(() -> restClient.get().uri(PATH + "/{id}", scheduleId)
                .headers(h -> actor(h, actorId, actorRole))
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(ScheduleDTO.class));
    }

    @Override
    public ScheduleChangeResultDTO open(OpenScheduleRequestDTO r) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resourceId", r.resourceId());
        payload.put("locationId", r.locationId());
        payload.put("modality", r.modality());
        payload.put("start", iso(r.start()));
        payload.put("end", iso(r.end()));
        payload.put("slotDurationMinutes", r.slotDurationMinutes());
        payload.put("capacityPerSlot", r.capacityPerSlot());
        return send(restClient.post().uri(PATH), payload, r.actorId(), r.actorRole());
    }

    @Override
    public ScheduleChangeResultDTO update(UpdateScheduleRequestDTO r) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("start", iso(r.start()));
        payload.put("end", iso(r.end()));
        payload.put("slotDurationMinutes", r.slotDurationMinutes());
        payload.put("capacityPerSlot", r.capacityPerSlot());
        payload.put("expectedVersion", r.expectedVersion());
        payload.put("confirmAffectedAppointments", r.confirmAffectedAppointments());
        payload.put("reason", r.reason());
        return send(restClient.put().uri(PATH + "/{id}", r.scheduleId()), payload, r.actorId(), r.actorRole());
    }

    @Override
    public ScheduleChangeResultDTO close(ScheduleStatusChangeRequestDTO r) {
        return send(restClient.post().uri(PATH + "/{id}/close", r.scheduleId()), statusPayload(r), r.actorId(),
                r.actorRole());
    }

    @Override
    public ScheduleChangeResultDTO reopen(ScheduleStatusChangeRequestDTO r) {
        return send(restClient.post().uri(PATH + "/{id}/reopen", r.scheduleId()), statusPayload(r), r.actorId(),
                r.actorRole());
    }

    private static Map<String, Object> statusPayload(ScheduleStatusChangeRequestDTO r) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("expectedVersion", r.expectedVersion());
        payload.put("confirmAffectedAppointments", r.confirmAffectedAppointments());
        payload.put("reason", r.reason());
        return payload;
    }

    private ScheduleChangeResultDTO send(RestClient.RequestBodySpec spec, Map<String, Object> payload,
                                         String actorId, String actorRole) {
        return call(() -> spec
                .headers(h -> actor(h, actorId, actorRole))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(ScheduleChangeResultDTO.class));
    }

    private static void actor(HttpHeaders headers, String actorId, String actorRole) {
        headers.set("X-Actor-Id", actorId == null ? "" : actorId);
        headers.set("X-Actor-Role", actorRole == null ? "" : actorRole);
    }

    private static String iso(java.time.LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private static Optional<String> nonBlank(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException ex) {
            throw new ScheduleClientException(ScheduleClientException.SERVER_UNREACHABLE,
                    "The server cannot be reached. Please retry in a moment.", List.of(), List.of(), ex);
        }
    }

    ScheduleClientException toException(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        JsonNode body = readBody(response);
        String code = text(body, "error");
        String message = text(body, "message");
        List<ScheduleSummaryDTO> conflicts = list(body, "conflictingSchedules", new TypeReference<>() {});
        List<AffectedAppointmentDTO> affected = list(body, "affectedAppointments", new TypeReference<>() {});
        if (code == null) {
            code = switch (status) {
                case 400 -> "INVALID_SCHEDULE";
                case 401, 403 -> "UNAUTHORIZED_OPERATION";
                case 404 -> "SCHEDULE_NOT_FOUND";
                default -> ScheduleClientException.GENERIC_ERROR;
            };
        }
        if (message == null) {
            message = "The schedule change could not be completed (HTTP " + status + ").";
        }
        return new ScheduleClientException(code, message, conflicts, affected, null);
    }

    private JsonNode readBody(ClientHttpResponse response) {
        try {
            String raw = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
            return raw.isBlank() ? null : objectMapper.readTree(raw);
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    private <T> List<T> list(JsonNode body, String field, TypeReference<List<T>> type) {
        if (body == null || !body.has(field) || !body.get(field).isArray()) {
            return List.of();
        }
        try {
            return objectMapper.convertValue(body.get(field), type);
        } catch (IllegalArgumentException ex) {
            return List.of();
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
