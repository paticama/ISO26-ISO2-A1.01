package com.sescam.radiodiagnostics.client.diagnostics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sescam.radiodiagnostics.common.diagnostics.DicomStudyDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ReportDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ReportableProcedureDTO;
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
import java.util.Optional;
import java.util.function.Supplier;

/**
 * REST client for the UC-PHY-04 server API ({@code /api/v1/reports}), using sescam-common DTOs.
 */
@Component
public class DiagnosticReportApiClient {

    static final String REPORTS_PATH = "/api/v1/reports";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public DiagnosticReportApiClient(RestClient.Builder builder, ObjectMapper objectMapper,
                                     @Value("${sescam.server.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public List<ReportableProcedureDTO> listPending(String actorId, String actorRole) {
        return call(() -> get(REPORTS_PATH + "/pending", actorId, actorRole)
                .body(new ParameterizedTypeReference<List<ReportableProcedureDTO>>() { }));
    }

    public DicomStudyDTO getDicomStudy(String executionId, String actorId, String actorRole) {
        return call(() -> get(REPORTS_PATH + "/executions/{executionId}/study", actorId, actorRole, executionId)
                .body(DicomStudyDTO.class));
    }

    public Optional<ReportDTO> findReportByExecutionId(String executionId, String actorId, String actorRole) {
        try {
            ReportDTO report = call(() -> get(REPORTS_PATH + "/executions/{executionId}", actorId, actorRole, executionId)
                    .body(ReportDTO.class));
            return Optional.ofNullable(report);
        } catch (DiagnosticReportClientException ex) {
            if (ex.getKind() == DiagnosticReportClientException.Kind.NOT_FOUND) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    public ReportDTO getReportById(String reportId, String actorId, String actorRole) {
        return call(() -> get(REPORTS_PATH + "/{reportId}", actorId, actorRole, reportId)
                .body(ReportDTO.class));
    }

    public ReportDTO saveDraft(DraftPayload payload, String actorId, String actorRole) {
        return call(() -> restClient.put()
                .uri(REPORTS_PATH + "/draft")
                .header("X-Actor-Id", nullToEmpty(actorId))
                .header("X-Actor-Role", nullToEmpty(actorRole))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(ReportDTO.class));
    }

    public ReportDTO finalizeAndSign(FinalizePayload payload, String actorId, String actorRole) {
        return post(REPORTS_PATH + "/finalize", payload, actorId, actorRole);
    }

    public ReportDTO issueAddendum(String reportId, AddendumPayload payload, String actorId, String actorRole) {
        return post(REPORTS_PATH + "/{reportId}/addendums", payload, actorId, actorRole, reportId);
    }

    private RestClient.ResponseSpec get(String uri, String actorId, String actorRole, Object... uriVars) {
        return restClient.get()
                .uri(uri, uriVars)
                .header("X-Actor-Id", nullToEmpty(actorId))
                .header("X-Actor-Role", nullToEmpty(actorRole))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                });
    }

    private ReportDTO post(String uri, Object payload, String actorId, String actorRole, Object... uriVars) {
        return call(() -> restClient.post()
                .uri(uri, uriVars)
                .header("X-Actor-Id", nullToEmpty(actorId))
                .header("X-Actor-Role", nullToEmpty(actorRole))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res);
                })
                .body(ReportDTO.class));
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException ex) {
            throw new DiagnosticReportClientException(
                    DiagnosticReportClientException.Kind.SERVER_UNREACHABLE,
                    "The server cannot be reached. Please retry in a moment.",
                    List.of(),
                    ex);
        }
    }

    DiagnosticReportClientException toException(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        JsonNode body = readBody(response);
        JsonNode messageNode = body == null ? null : body.get("message");
        String message = messageNode == null || messageNode.isNull() ? null : messageNode.asText();
        DiagnosticReportClientException.Kind kind = switch (status) {
            case 401, 403 -> DiagnosticReportClientException.Kind.FORBIDDEN;
            case 400 -> DiagnosticReportClientException.Kind.INVALID_DATA;
            case 404 -> DiagnosticReportClientException.Kind.NOT_FOUND;
            case 409 -> DiagnosticReportClientException.Kind.STATE_CONFLICT;
            case 502 -> DiagnosticReportClientException.Kind.PACS_ERROR;
            default -> DiagnosticReportClientException.Kind.FAILED;
        };
        if (message == null) {
            message = switch (kind) {
                case FORBIDDEN -> "You are not authorized to access DICOM studies or author diagnostic reports.";
                case INVALID_DATA -> "The report data is invalid. Check the highlighted fields.";
                case NOT_FOUND -> "The requested procedure or diagnostic report was not found.";
                case STATE_CONFLICT -> "The report state or version conflicts with your submission.";
                case PACS_ERROR -> "Could not connect to the departmental RIS/PACS image archive.";
                default -> "The diagnostic report request failed. Please retry.";
            };
        }
        List<String> invalidFields = new ArrayList<>();
        JsonNode fields = body == null ? null : body.get("invalidFields");
        if (kind == DiagnosticReportClientException.Kind.INVALID_DATA && fields != null && fields.isArray()) {
            fields.forEach(f -> invalidFields.add(f.asText()));
        }
        return new DiagnosticReportClientException(kind, message, invalidFields, null);
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

    public record DraftPayload(
            String executionId,
            String findings,
            String conclusion,
            boolean criticalAlert,
            boolean followUpRecommended,
            Long expectedVersion
    ) {
    }

    public record FinalizePayload(
            String executionId,
            String findings,
            String conclusion,
            boolean criticalAlert,
            boolean followUpRecommended,
            String digitalSignature,
            Long expectedVersion
    ) {
    }

    public record AddendumPayload(
            String reason,
            String addendumContent,
            String digitalSignature,
            Long expectedVersion
    ) {
    }
}
