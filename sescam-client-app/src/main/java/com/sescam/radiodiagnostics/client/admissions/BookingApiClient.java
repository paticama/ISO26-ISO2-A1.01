package com.sescam.radiodiagnostics.client.admissions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sescam.radiodiagnostics.common.admissions.BookingDTO;
import com.sescam.radiodiagnostics.common.admissions.PendingExamRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.WorklistItemDTO;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
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
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/**
 * REST client for UC-AAD-05 (/api/v1/bookings).
 */
@Component
public class BookingApiClient {

    static final String BOOKINGS_PATH = "/api/v1/bookings";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public BookingApiClient(RestClient.Builder builder, ObjectMapper objectMapper,
                            @Value("${sescam.server.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public List<PendingExamRequestDTO> listPendingOrders(String actorId, String actorRole) {
        return call(() -> restClient.get()
                .uri(BOOKINGS_PATH + "/pending-orders")
                .header("X-Actor-Id", nullToEmpty(actorId))
                .header("X-Actor-Role", nullToEmpty(actorRole))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { throw toException(res); })
                .body(new ParameterizedTypeReference<>() { }));
    }

    public List<SlotDTO> findCompatibleSlots(String orderId, String actorId, String actorRole) {
        return call(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path(BOOKINGS_PATH + "/slots").queryParam("orderId", orderId).build())
                .header("X-Actor-Id", nullToEmpty(actorId))
                .header("X-Actor-Role", nullToEmpty(actorRole))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { throw toException(res); })
                .body(new ParameterizedTypeReference<>() { }));
    }

    public BookingDTO createBooking(CreateBookingPayload payload, String actorId, String actorRole) {
        return call(() -> restClient.post()
                .uri(BOOKINGS_PATH)
                .header("X-Actor-Id", nullToEmpty(actorId))
                .header("X-Actor-Role", nullToEmpty(actorRole))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { throw toException(res); })
                .body(BookingDTO.class));
    }

    public BookingDTO getBooking(String bookingId, String actorId, String actorRole) {
        return call(() -> restClient.get()
                .uri(BOOKINGS_PATH + "/{id}", bookingId)
                .header("X-Actor-Id", nullToEmpty(actorId))
                .header("X-Actor-Role", nullToEmpty(actorRole))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { throw toException(res); })
                .body(BookingDTO.class));
    }

    public BookingDTO rescheduleBooking(String bookingId, ReschedulePayload payload,
                                        String actorId, String actorRole) {
        return call(() -> restClient.post()
                .uri(BOOKINGS_PATH + "/{id}/reschedule", bookingId)
                .header("X-Actor-Id", nullToEmpty(actorId))
                .header("X-Actor-Role", nullToEmpty(actorRole))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { throw toException(res); })
                .body(BookingDTO.class));
    }

    public BookingDTO cancelBooking(String bookingId, CancelPayload payload,
                                    String actorId, String actorRole) {
        return call(() -> restClient.post()
                .uri(BOOKINGS_PATH + "/{id}/cancel", bookingId)
                .header("X-Actor-Id", nullToEmpty(actorId))
                .header("X-Actor-Role", nullToEmpty(actorRole))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { throw toException(res); })
                .body(BookingDTO.class));
    }

    public List<WorklistItemDTO> listWorklist(LocalDate date, String actorId, String actorRole) {
        return call(() -> restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path(BOOKINGS_PATH + "/worklist");
                    if (date != null) {
                        builder.queryParam("date", date.toString());
                    }
                    return builder.build();
                })
                .header("X-Actor-Id", nullToEmpty(actorId))
                .header("X-Actor-Role", nullToEmpty(actorRole))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { throw toException(res); })
                .body(new ParameterizedTypeReference<>() { }));
    }

    public record CreateBookingPayload(String orderId, String slotId, long expectedSlotVersion) {}

    public record ReschedulePayload(String newSlotId, long expectedSlotVersion, long expectedBookingVersion, String reason) {}

    public record CancelPayload(long expectedBookingVersion, String reason) {}

    private static <T> T call(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (ResourceAccessException ex) {
            throw new BookingClientException("SERVER_UNAVAILABLE", 503,
                    "Admissions server is unreachable.", List.of(), List.of());
        }
    }

    private BookingClientException toException(ClientHttpResponse res) throws IOException {
        int status = res.getStatusCode().value();
        String body = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
        String code = "BOOKING_ERROR";
        String message = "Request failed with status " + status;
        List<String> invalidFields = List.of();
        List<SlotDTO> alternatives = List.of();
        if (!body.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(body);
                if (root.hasNonNull("code")) code = root.get("code").asText();
                if (root.hasNonNull("message")) message = root.get("message").asText();
                if (root.has("invalidFields") && root.get("invalidFields").isArray()) {
                    invalidFields = objectMapper.convertValue(root.get("invalidFields"), new TypeReference<>() { });
                }
                if (root.has("alternativeSlots") && root.get("alternativeSlots").isArray()) {
                    alternatives = objectMapper.convertValue(root.get("alternativeSlots"), new TypeReference<>() { });
                }
            } catch (Exception ignored) {
                // Keep defaults when server returns non-JSON body.
            }
        }
        return new BookingClientException(code, status, message, invalidFields, alternatives);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
