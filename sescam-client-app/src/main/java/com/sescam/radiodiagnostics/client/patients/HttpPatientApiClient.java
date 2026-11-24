package com.sescam.radiodiagnostics.client.patients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sescam.radiodiagnostics.common.patients.CreatePatientRequestDTO;
import com.sescam.radiodiagnostics.common.patients.DischargePatientRequestDTO;
import com.sescam.radiodiagnostics.common.patients.PatientDTO;
import com.sescam.radiodiagnostics.common.patients.PatientStatus;
import com.sescam.radiodiagnostics.common.patients.PatientSummaryDTO;
import com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO;
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

@Component
public class HttpPatientApiClient implements PatientApiClient {

    static final String PATH = "/api/v1/patients";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpPatientApiClient(RestClient.Builder builder, ObjectMapper objectMapper,
                                @Value("${sescam.server.base-url:http://localhost:8080}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<PatientSummaryDTO> search(String query, PatientStatus status, int page, int size,
                                         String actorId, String actorRole) {
        return call(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path(PATH)
                        .queryParam("query", query == null ? "" : query)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .queryParamIfPresent("status", java.util.Optional.ofNullable(status))
                        .build())
                .headers(h -> actor(h, actorId, actorRole))
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(new ParameterizedTypeReference<List<PatientSummaryDTO>>() {}));
    }

    @Override
    public PatientDTO getById(String id, String actorId, String actorRole) {
        return call(() -> restClient.get().uri(PATH + "/{id}", id)
                .headers(h -> actor(h, actorId, actorRole))
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(PatientDTO.class));
    }

    @Override
    public PatientDTO getByCip(String cip, String actorId, String actorRole) {
        return call(() -> restClient.get().uri(PATH + "/cip/{cip}", cip)
                .headers(h -> actor(h, actorId, actorRole))
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(PatientDTO.class));
    }

    @Override
    public PatientDTO register(CreatePatientRequestDTO request) {
        Map<String, Object> payload = Map.of(
                "cip", request.cip(),
                "dni", request.dni(),
                "firstName", request.firstName(),
                "lastName", request.lastName(),
                "birthDate", request.birthDate().toString(),
                "gender", request.gender() != null ? request.gender().name() : "UNKNOWN",
                "phone", request.phone() == null ? "" : request.phone(),
                "email", request.email() == null ? "" : request.email()
        );
        return call(() -> restClient.post().uri(PATH)
                .headers(h -> actor(h, request.actorId(), request.actorRole()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(PatientDTO.class));
    }

    @Override
    public PatientDTO update(UpdatePatientRequestDTO request) {
        Map<String, Object> payload = Map.of(
                "firstName", request.firstName() == null ? "" : request.firstName(),
                "lastName", request.lastName() == null ? "" : request.lastName(),
                "phone", request.phone() == null ? "" : request.phone(),
                "email", request.email() == null ? "" : request.email(),
                "clinicalSummaryNotes", request.clinicalSummaryNotes() == null ? "" : request.clinicalSummaryNotes(),
                "expectedVersion", request.expectedVersion()
        );
        return call(() -> restClient.put().uri(PATH + "/{id}", request.patientId())
                .headers(h -> actor(h, request.actorId(), request.actorRole()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(PatientDTO.class));
    }

    @Override
    public PatientDTO discharge(DischargePatientRequestDTO request) {
        Map<String, Object> payload = Map.of(
                "dischargeReason", request.dischargeReason() == null ? "" : request.dischargeReason(),
                "expectedVersion", request.expectedVersion()
        );
        return call(() -> restClient.post().uri(PATH + "/{id}/discharge", request.patientId())
                .headers(h -> actor(h, request.actorId(), request.actorRole()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(PatientDTO.class));
    }

    private static void actor(org.springframework.http.HttpHeaders headers, String actorId, String actorRole) {
        headers.set("X-Actor-Id", actorId == null ? "" : actorId);
        headers.set("X-Actor-Role", actorRole == null ? "" : actorRole);
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException ex) {
            throw new PatientClientException(PatientClientException.Kind.SERVER_UNREACHABLE,
                    "The server cannot be reached. Please retry in a moment.", List.of(), ex);
        }
    }

    PatientClientException toException(ClientHttpResponse response) {
        try {
            int status = response.getStatusCode().value();
            JsonNode body = readBody(response);
            String message = text(body, "message");
            List<String> errors = new ArrayList<>();
            if (body != null && body.has("errors") && body.get("errors").isArray()) {
                body.get("errors").forEach(e -> errors.add(e.asText()));
            }

            PatientClientException.Kind kind = switch (status) {
                case 401, 403 -> PatientClientException.Kind.FORBIDDEN;
                case 404 -> PatientClientException.Kind.NOT_FOUND;
                case 400 -> PatientClientException.Kind.INVALID_DATA;
                case 409 -> PatientClientException.Kind.CONFLICT;
                case 502 -> PatientClientException.Kind.EHR_SYNC_FAILED;
                default -> PatientClientException.Kind.GENERIC_ERROR;
            };

            if (message == null) {
                message = switch (kind) {
                    case FORBIDDEN -> "You are not authorized to manage patient records.";
                    case NOT_FOUND -> "The requested patient was not found.";
                    case INVALID_DATA -> "The provided patient data is invalid.";
                    case CONFLICT -> "A duplicate record exists or concurrent edit occurred.";
                    case EHR_SYNC_FAILED -> "External EHR synchronization failed.";
                    default -> "The request could not be completed. Please retry.";
                };
            }
            return new PatientClientException(kind, message, errors, null);
        } catch (IOException ex) {
            return new PatientClientException(PatientClientException.Kind.GENERIC_ERROR, "Failed to read server response", List.of(), ex);
        }
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
