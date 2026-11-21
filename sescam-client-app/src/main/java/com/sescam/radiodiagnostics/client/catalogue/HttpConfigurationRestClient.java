package com.sescam.radiodiagnostics.client.catalogue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sescam.radiodiagnostics.common.catalogue.ConcurrentConfigurationChangeException;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationNotFoundException;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationParameterDTO;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationSaveException;
import com.sescam.radiodiagnostics.common.catalogue.InvalidConfigurationValueException;
import com.sescam.radiodiagnostics.common.catalogue.UnauthorizedConfigurationAccessException;
import com.sescam.radiodiagnostics.common.catalogue.UpdateParameterRequestDTO;
import com.sescam.radiodiagnostics.common.catalogue.UpdateParameterResponseDTO;
import com.sescam.radiodiagnostics.common.catalogue.ValidationResultDTO;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP REST client consuming the sescam-server-app configuration endpoints.
 */
public class HttpConfigurationRestClient implements ConfigurationRestClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpConfigurationRestClient(String baseUrl) {
        this(baseUrl, HttpClient.newHttpClient());
    }

    public HttpConfigurationRestClient(String baseUrl, HttpClient httpClient) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null").replaceAll("/+$", "");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public List<ConfigurationParameterDTO> fetchAllParameters(String actorId, String actorRole) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/configurations"))
                .header("X-Actor-Id", safeHeader(actorId))
                .header("X-Actor-Role", safeHeader(actorRole))
                .GET()
                .build();

        HttpResponse<String> response = send(request, "list", "");
        if (response.statusCode() == 200) {
            try {
                return objectMapper.readValue(response.body(), new TypeReference<>() {});
            } catch (IOException ex) {
                throw new ConfigurationSaveException("list", "", ex);
            }
        }
        throw mapError(response, "list", "", actorId);
    }

    @Override
    public ValidationResultDTO validateRemotely(String parameterKey, String proposedValue,
                                                String actorId, String actorRole) {
        String encodedKey = URLEncoder.encode(parameterKey, StandardCharsets.UTF_8);
        String body = writeJson(Map.of("proposedValue", proposedValue == null ? "" : proposedValue));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/configurations/" + encodedKey + "/validate"))
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", safeHeader(actorId))
                .header("X-Actor-Role", safeHeader(actorRole))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = send(request, parameterKey, "");
        if (response.statusCode() == 200) {
            try {
                return objectMapper.readValue(response.body(), ValidationResultDTO.class);
            } catch (IOException ex) {
                throw new ConfigurationSaveException(parameterKey, "", ex);
            }
        }
        throw mapError(response, parameterKey, "", actorId);
    }

    @Override
    public UpdateParameterResponseDTO updateParameter(UpdateParameterRequestDTO dto) {
        Objects.requireNonNull(dto, "dto must not be null");
        String encodedKey = URLEncoder.encode(dto.parameterKey(), StandardCharsets.UTF_8);
        String body = writeJson(Map.of(
                "proposedValue", dto.proposedValue() == null ? "" : dto.proposedValue(),
                "expectedVersion", dto.expectedVersion()
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/configurations/" + encodedKey))
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", safeHeader(dto.administratorId()))
                .header("X-Actor-Role", safeHeader(dto.administratorRole()))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = send(request, dto.parameterKey(), "");
        if (response.statusCode() == 200) {
            try {
                return objectMapper.readValue(response.body(), UpdateParameterResponseDTO.class);
            } catch (IOException ex) {
                throw new ConfigurationSaveException(dto.parameterKey(), "", ex);
            }
        }
        throw mapError(response, dto.parameterKey(), "", dto.administratorId());
    }

    private HttpResponse<String> send(HttpRequest request, String key, String previousValue) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ConfigurationSaveException(key, previousValue, ex);
        }
    }

    private RuntimeException mapError(HttpResponse<String> response, String key, String fallbackPrev, String actorId) {
        JsonNode json = parseJsonSafely(response.body());
        String msg = json.path("message").asText("Server returned HTTP " + response.statusCode());

        return switch (response.statusCode()) {
            case 400 -> new InvalidConfigurationValueException(key, msg);
            case 403 -> new UnauthorizedConfigurationAccessException(actorId, msg);
            case 404 -> new ConfigurationNotFoundException(key);
            case 409 -> new ConcurrentConfigurationChangeException(
                    json.path("parameterKey").asText(key),
                    json.path("expectedVersion").asLong(0L),
                    json.path("actualVersion").asLong(0L),
                    json.path("currentStoredValue").asText("")
            );
            default -> new ConfigurationSaveException(
                    key,
                    json.path("previousValue").asText(fallbackPrev),
                    new IllegalStateException(msg)
            );
        };
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to serialize request payload", ex);
        }
    }

    private JsonNode parseJsonSafely(String body) {
        try {
            return body == null || body.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(body);
        } catch (IOException ex) {
            return objectMapper.createObjectNode();
        }
    }

    private String safeHeader(String value) {
        return value == null ? "" : value;
    }
}
