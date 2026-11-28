package com.sescam.radiodiagnostics.client.orders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sescam.radiodiagnostics.common.catalogue.ExamTypeDTO;
import com.sescam.radiodiagnostics.common.orders.OrderResponseDTO;
import com.sescam.radiodiagnostics.common.orders.SafetyConstraintDTO;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.function.Supplier;

/**
 * REST client of the UC-PHY-01 server API ({@code /api/v1/orders}), using the DTOs of sescam-common.
 */
@Component
public class OrderApiClient {

    static final String ORDERS_PATH = "/api/v1/orders";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OrderApiClient(RestClient.Builder builder, ObjectMapper objectMapper,
                          @Value("${sescam.server.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    /** Steps 1-3: the active catalogue of examination types. */
    public List<ExamTypeDTO> listExamTypes() {
        return call(() -> restClient.get()
                .uri(ORDERS_PATH + "/exam-types")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(new org.springframework.core.ParameterizedTypeReference<List<ExamTypeDTO>>() { }));
    }

    /**
     * Steps 5-8. A safety alert (6b) is thrown as an {@link OrderClientException} of kind SAFETY_ALERT that carries
     * the constraints; the order is then sent again with the override justification.
     */
    public OrderResponseDTO createOrder(OrderPayload payload, String actorId, String actorRole) {
        return call(() -> restClient.post()
                .uri(ORDERS_PATH)
                .header("X-Actor-Id", nullToEmpty(actorId))
                .header("X-Actor-Role", nullToEmpty(actorRole))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(OrderResponseDTO.class));
    }

    /** Request body of the server API; the actor travels in the headers. */
    public record OrderPayload(
            String patientId,
            String examTypeCode,
            String clinicalIndication,
            String clinicalJustification,
            String priority,
            String overrideJustification,
            List<String> acknowledgedConstraintIds
    ) {
        public OrderPayload(String patientId, String examTypeCode, String clinicalIndication,
                            String clinicalJustification, String priority, String overrideJustification) {
            this(patientId, examTypeCode, clinicalIndication, clinicalJustification, priority,
                    overrideJustification, List.of());
        }
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException ex) {
            throw new OrderClientException(OrderClientException.Kind.SERVER_UNREACHABLE,
                    "The server cannot be reached. The order has not been created. Please retry in a moment.", ex);
        }
    }

    OrderClientException toException(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        JsonNode body = readBody(response);
        String message = text(body, "message");
        OrderClientException.Kind kind = switch (status) {
            case 401, 403 -> OrderClientException.Kind.FORBIDDEN;
            case 400 -> OrderClientException.Kind.INVALID_DATA;
            case 404 -> OrderClientException.Kind.EXAM_NOT_FOUND;
            case 422 -> OrderClientException.Kind.SAFETY_ALERT;
            default -> OrderClientException.Kind.FAILED;
        };
        if (message == null) {
            message = switch (kind) {
                case FORBIDDEN -> "Only physicians can order diagnostic procedures.";
                case INVALID_DATA -> "The order data is not valid. Check the highlighted fields.";
                case EXAM_NOT_FOUND -> "The examination type does not exist or is no longer active.";
                case SAFETY_ALERT -> "The patient has safety constraints incompatible with this examination.";
                default -> "The order could not be created. Please retry.";
            };
        }
        List<SafetyConstraintDTO> constraints = kind == OrderClientException.Kind.SAFETY_ALERT
                ? constraints(body) : List.of();
        List<String> invalidFields = kind == OrderClientException.Kind.INVALID_DATA
                ? strings(body, "invalidFields") : List.of();
        return new OrderClientException(kind, message, constraints, invalidFields, null);
    }

    private List<SafetyConstraintDTO> constraints(JsonNode body) {
        try {
            JsonNode node = body == null ? null : body.get("constraints");
            if (node == null || !node.isArray()) {
                return List.of();
            }
            return objectMapper.convertValue(node, new TypeReference<List<SafetyConstraintDTO>>() { });
        } catch (IllegalArgumentException ex) {
            return List.of();
        }
    }

    private static List<String> strings(JsonNode body, String field) {
        List<String> values = new ArrayList<>();
        JsonNode node = body == null ? null : body.get(field);
        if (node != null && node.isArray()) {
            node.forEach(n -> values.add(n.asText()));
        }
        return values;
    }

    private JsonNode readBody(ClientHttpResponse response) {
        try {
            return objectMapper.readTree(new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    private static String text(JsonNode body, String field) {
        JsonNode node = body == null ? null : body.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
