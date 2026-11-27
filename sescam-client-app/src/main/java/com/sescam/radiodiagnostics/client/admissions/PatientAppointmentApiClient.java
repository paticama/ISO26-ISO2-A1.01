package com.sescam.radiodiagnostics.client.admissions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sescam.radiodiagnostics.common.admissions.AppointmentDetailsDTO;
import com.sescam.radiodiagnostics.common.admissions.AppointmentSummaryDTO;
import com.sescam.radiodiagnostics.common.admissions.RescheduleRequestStatusDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/** REST client for UC-PAT-01's patient-owned appointment endpoints. */
@Component
public class PatientAppointmentApiClient {

    static final String PATH = "/api/v1/my-appointments";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PatientAppointmentApiClient(RestClient.Builder builder, ObjectMapper objectMapper,
                                       @Value("${sescam.server.base-url:http://localhost:8080}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public List<AppointmentSummaryDTO> list(String actorId, String actorRole) {
        return call(() -> restClient.get().uri(PATH)
                .headers(h -> actor(h, actorId, actorRole))
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> { throw toException(res); })
                .body(new ParameterizedTypeReference<List<AppointmentSummaryDTO>>() { }));
    }

    public AppointmentDetailsDTO get(String bookingId, String actorId, String actorRole) {
        return call(() -> restClient.get().uri(PATH + "/{id}", bookingId)
                .headers(h -> actor(h, actorId, actorRole))
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> { throw toException(res); })
                .body(AppointmentDetailsDTO.class));
    }

    public AppointmentDetailsDTO confirm(String bookingId, long expectedVersion, String actorId, String actorRole) {
        return call(() -> restClient.post().uri(PATH + "/{id}/confirm", bookingId)
                .headers(h -> actor(h, actorId, actorRole))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ConfirmPayload(expectedVersion))
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> { throw toException(res); })
                .body(AppointmentDetailsDTO.class));
    }

    public RescheduleRequestStatusDTO requestRescheduling(String bookingId, String reason,
                                                           List<LocalDate> preferredDates,
                                                           String actorId, String actorRole) {
        return call(() -> restClient.post().uri(PATH + "/{id}/reschedule-requests", bookingId)
                .headers(h -> actor(h, actorId, actorRole))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ReschedulePayload(reason, preferredDates == null ? List.of() : preferredDates))
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> { throw toException(res); })
                .body(RescheduleRequestStatusDTO.class));
    }

    public AppointmentDetailsDTO cancel(String bookingId, long expectedVersion, String reason,
                                        boolean priorityWarningAcknowledged,
                                        String actorId, String actorRole) {
        return call(() -> restClient.post().uri(PATH + "/{id}/cancel", bookingId)
                .headers(h -> actor(h, actorId, actorRole))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CancelPayload(expectedVersion, reason, priorityWarningAcknowledged))
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> { throw toException(res); })
                .body(AppointmentDetailsDTO.class));
    }

    public record ConfirmPayload(long expectedBookingVersion) { }
    public record ReschedulePayload(String reason, List<LocalDate> preferredDates) { }
    public record CancelPayload(long expectedBookingVersion, String reason, boolean priorityWarningAcknowledged) { }

    private static void actor(org.springframework.http.HttpHeaders headers, String actorId, String actorRole) {
        headers.set("X-Actor-Id", actorId == null ? "" : actorId);
        headers.set("X-Actor-Role", actorRole == null ? "" : actorRole);
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (BookingClientException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw new BookingClientException("SERVER_UNAVAILABLE", 503,
                    "Admissions server is unreachable. Please try again.", List.of(), List.of());
        } catch (RestClientException ex) {
            throw new BookingClientException("CLIENT_ERROR", 502,
                    "Unexpected response from admissions server.", List.of(), List.of());
        }
    }

    private BookingClientException toException(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        String code = "APPOINTMENT_ERROR";
        String message = "Appointment request failed with status " + status;
        List<String> invalidFields = List.of();
        if (!body.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(body);
                if (root.hasNonNull("code")) code = root.get("code").asText();
                if (root.hasNonNull("message")) message = root.get("message").asText();
                if (root.has("invalidFields") && root.get("invalidFields").isArray()) {
                    invalidFields = objectMapper.convertValue(root.get("invalidFields"), new TypeReference<>() { });
                }
            } catch (Exception ignored) {
                // Preserve safe defaults for non-JSON error responses.
            }
        }
        return new BookingClientException(code, status, message, invalidFields, List.of());
    }
}
