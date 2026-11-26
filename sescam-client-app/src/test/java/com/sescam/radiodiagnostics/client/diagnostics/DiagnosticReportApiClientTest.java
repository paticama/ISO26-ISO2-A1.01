package com.sescam.radiodiagnostics.client.diagnostics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.diagnostics.DicomStudyDTO;
import com.sescam.radiodiagnostics.common.diagnostics.EhrReportSyncStatus;
import com.sescam.radiodiagnostics.common.diagnostics.ReportDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ReportStatus;
import com.sescam.radiodiagnostics.common.diagnostics.ReportableProcedureDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DiagnosticReportApiClientTest {

    private static final String BASE = "http://server";

    private MockRestServiceServer server;
    private DiagnosticReportApiClient client;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RestClient.Builder builder = RestClient.builder()
                .messageConverters(c -> c.add(0, new MappingJackson2HttpMessageConverter(mapper)));
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DiagnosticReportApiClient(builder, mapper, BASE);
    }

    @Test
    void listsPendingAndFetchesStudyAndReport() {
        server.expect(requestTo(BASE + "/api/v1/reports/pending"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Actor-Id", "dr-1"))
                .andExpect(header("X-Actor-Role", "PHYSICIAN"))
                .andRespond(withSuccess("""
                        [{"executionId":"EXE-1","orderId":"ORD-1","patientId":"PAT-1",
                          "examTypeName":"Abdominal CT","modality":"CT",
                          "executedAt":"2026-11-26T09:25:00Z","reportId":null,"reportStatus":null}]
                        """, MediaType.APPLICATION_JSON));

        List<ReportableProcedureDTO> list = client.listPending("dr-1", "PHYSICIAN");
        assertThat(list).singleElement().satisfies(p -> {
            assertThat(p.executionId()).isEqualTo("EXE-1");
            assertThat(p.modality()).isEqualTo(ModalityType.CT);
        });
        server.verify();
        server.reset();

        server.expect(requestTo(BASE + "/api/v1/reports/executions/EXE-1/study"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"studyInstanceUid":"1.2.3","executionId":"EXE-1","orderId":"ORD-1","patientId":"PAT-1",
                         "accessionNumber":"ACC-1","modality":"CT","examTypeName":"Abdominal CT",
                         "numberOfSeries":4,"numberOfInstances":180,"wadoViewerToken":"wado-exe-1",
                         "pacsEndpointUrl":"https://pacs.sescam.local/wado-rs/studies/1.2.3",
                         "acquiredAt":"2026-11-26T09:25:00Z"}
                        """, MediaType.APPLICATION_JSON));

        DicomStudyDTO study = client.getDicomStudy("EXE-1", "dr-1", "PHYSICIAN");
        assertThat(study.wadoViewerToken()).isEqualTo("wado-exe-1");
        server.verify();
        server.reset();

        server.expect(requestTo(BASE + "/api/v1/reports/executions/EXE-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThat(client.findReportByExecutionId("EXE-1", "dr-1", "PHYSICIAN")).isEmpty();
        server.verify();
        server.reset();

        server.expect(requestTo(BASE + "/api/v1/reports/REP-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"reportId":"REP-1","executionId":"EXE-1","orderId":"ORD-1","patientId":"PAT-1",
                         "studyInstanceUid":"1.2.3","examTypeName":"Abdominal CT","modality":"CT",
                         "findings":"F","conclusion":"C","criticalAlert":false,"followUpRecommended":false,
                         "status":"FINALIZED","authorPhysicianId":"dr-1","digitalSignature":"SIG-1",
                         "signedAt":"2026-11-26T10:00:00Z","ehrSyncStatus":"SYNCED",
                         "ehrSyncedAt":"2026-11-26T10:00:00Z","version":2,
                         "createdAt":"2026-11-26T09:50:00Z","updatedAt":"2026-11-26T10:00:00Z","addendums":[]}
                        """, MediaType.APPLICATION_JSON));
        ReportDTO loaded = client.getReportById("REP-1", "dr-1", "PHYSICIAN");
        assertThat(loaded.status()).isEqualTo(ReportStatus.FINALIZED);
        server.verify();
    }

    @Test
    void draftFinalizeAndAddendumEndpointsSendExpectedPayloads() {
        server.expect(requestTo(BASE + "/api/v1/reports/draft"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.executionId").value("EXE-1"))
                .andRespond(withSuccess("""
                        {"reportId":"REP-1","executionId":"EXE-1","orderId":"ORD-1","patientId":"PAT-1",
                         "studyInstanceUid":"1.2.3","examTypeName":"Abdominal CT","modality":"CT",
                         "findings":"Draft","conclusion":"","criticalAlert":false,"followUpRecommended":false,
                         "status":"DRAFT","authorPhysicianId":"dr-1","digitalSignature":null,"signedAt":null,
                         "ehrSyncStatus":"NOT_SYNCED","ehrSyncedAt":null,"version":1,
                         "createdAt":"2026-11-26T09:50:00Z","updatedAt":"2026-11-26T09:50:00Z","addendums":[]}
                        """, MediaType.APPLICATION_JSON));

        ReportDTO draft = client.saveDraft(
                new DiagnosticReportApiClient.DraftPayload("EXE-1", "Draft", "", false, false, null),
                "dr-1", "PHYSICIAN");
        assertThat(draft.ehrSyncStatus()).isEqualTo(EhrReportSyncStatus.NOT_SYNCED);
        server.verify();
        server.reset();

        server.expect(requestTo(BASE + "/api/v1/reports/finalize"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.digitalSignature").value("SIG-1"))
                .andRespond(withSuccess("""
                        {"reportId":"REP-1","executionId":"EXE-1","orderId":"ORD-1","patientId":"PAT-1",
                         "studyInstanceUid":"1.2.3","examTypeName":"Abdominal CT","modality":"CT",
                         "findings":"Final","conclusion":"Normal","criticalAlert":true,"followUpRecommended":true,
                         "status":"FINALIZED","authorPhysicianId":"dr-1","digitalSignature":"SIG-1",
                         "signedAt":"2026-11-26T10:00:00Z","ehrSyncStatus":"SYNCED",
                         "ehrSyncedAt":"2026-11-26T10:00:00Z","version":2,
                         "createdAt":"2026-11-26T09:50:00Z","updatedAt":"2026-11-26T10:00:00Z","addendums":[]}
                        """, MediaType.APPLICATION_JSON));

        ReportDTO finalized = client.finalizeAndSign(
                new DiagnosticReportApiClient.FinalizePayload("EXE-1", "Final", "Normal", true, true, "SIG-1", 1L),
                "dr-1", "RADIOLOGIST");
        assertThat(finalized.criticalAlert()).isTrue();
        server.verify();
        server.reset();

        server.expect(requestTo(BASE + "/api/v1/reports/REP-1/addendums"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.reason").value("Clarification"))
                .andRespond(withSuccess("""
                        {"reportId":"REP-1","executionId":"EXE-1","orderId":"ORD-1","patientId":"PAT-1",
                         "studyInstanceUid":"1.2.3","examTypeName":"Abdominal CT","modality":"CT",
                         "findings":"Final","conclusion":"Normal","criticalAlert":true,"followUpRecommended":true,
                         "status":"AMENDED","authorPhysicianId":"dr-1","digitalSignature":"SIG-1",
                         "signedAt":"2026-11-26T10:00:00Z","ehrSyncStatus":"SYNCED",
                         "ehrSyncedAt":"2026-11-26T10:00:00Z","version":3,
                         "createdAt":"2026-11-26T09:50:00Z","updatedAt":"2026-11-26T10:10:00Z",
                         "addendums":[{"addendumId":"ADD-1","reason":"Clarification","content":"Note",
                                       "authorPhysicianId":"dr-2","digitalSignature":"SIG-2",
                                       "createdAt":"2026-11-26T10:10:00Z"}]}
                        """, MediaType.APPLICATION_JSON));

        ReportDTO amended = client.issueAddendum("REP-1",
                new DiagnosticReportApiClient.AddendumPayload("Clarification", "Note", "SIG-2", 2L),
                "dr-2", "RADIOLOGIST");
        assertThat(amended.status()).isEqualTo(ReportStatus.AMENDED);
        server.verify();
    }

    @Test
    void mapsHttpAndNetworkErrorsToTypedClientExceptions() {
        server.expect(requestTo(BASE + "/api/v1/reports/pending"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> client.listPending("tech-1", "RADIOLOGY_TECHNICIAN"))
                .isInstanceOf(DiagnosticReportClientException.class)
                .satisfies(ex -> assertThat(((DiagnosticReportClientException) ex).getKind())
                        .isEqualTo(DiagnosticReportClientException.Kind.FORBIDDEN));
        server.verify();
        server.reset();

        server.expect(requestTo(BASE + "/api/v1/reports/executions/EXE-1/study"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"PACS timeout\"}"));
        assertThatThrownBy(() -> client.getDicomStudy("EXE-1", "dr-1", "PHYSICIAN"))
                .isInstanceOf(DiagnosticReportClientException.class)
                .satisfies(ex -> {
                    DiagnosticReportClientException ce = (DiagnosticReportClientException) ex;
                    assertThat(ce.getKind()).isEqualTo(DiagnosticReportClientException.Kind.PACS_ERROR);
                    assertThat(ce.canRetry()).isTrue();
                    assertThat(ce.getMessage()).isEqualTo("PACS timeout");
                });
        server.verify();
        server.reset();

        server.expect(requestTo(BASE + "/api/v1/reports/finalize"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Invalid\",\"invalidFields\":[\"digitalSignature\"]}"));
        assertThatThrownBy(() -> client.finalizeAndSign(
                new DiagnosticReportApiClient.FinalizePayload("EXE-1", "F", "C", false, false, "", null),
                "dr-1", "PHYSICIAN"))
                .isInstanceOf(DiagnosticReportClientException.class)
                .satisfies(ex -> {
                    DiagnosticReportClientException ce = (DiagnosticReportClientException) ex;
                    assertThat(ce.getKind()).isEqualTo(DiagnosticReportClientException.Kind.INVALID_DATA);
                    assertThat(ce.getInvalidFields()).containsExactly("digitalSignature");
                });
        server.verify();
        server.reset();

        server.expect(requestTo(BASE + "/api/v1/reports/draft"))
                .andRespond(withStatus(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> client.saveDraft(
                new DiagnosticReportApiClient.DraftPayload("EXE-1", "F", "C", false, false, 1L),
                "dr-1", "PHYSICIAN"))
                .isInstanceOf(DiagnosticReportClientException.class)
                .satisfies(ex -> assertThat(((DiagnosticReportClientException) ex).getKind())
                        .isEqualTo(DiagnosticReportClientException.Kind.STATE_CONFLICT));
        server.verify();
        server.reset();

        server.expect(requestTo(BASE + "/api/v1/reports/pending"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThatThrownBy(() -> client.listPending("dr-1", "PHYSICIAN"))
                .isInstanceOf(DiagnosticReportClientException.class)
                .satisfies(ex -> assertThat(((DiagnosticReportClientException) ex).getKind())
                        .isEqualTo(DiagnosticReportClientException.Kind.FAILED));
        server.verify();
        server.reset();

        server.expect(requestTo(BASE + "/api/v1/reports/pending"))
                .andRespond(withException(new IOException("Connection refused")));
        assertThatThrownBy(() -> client.listPending("dr-1", "PHYSICIAN"))
                .isInstanceOf(DiagnosticReportClientException.class)
                .satisfies(ex -> {
                    DiagnosticReportClientException ce = (DiagnosticReportClientException) ex;
                    assertThat(ce.getKind()).isEqualTo(DiagnosticReportClientException.Kind.SERVER_UNREACHABLE);
                    assertThat(ce.canRetry()).isTrue();
                });
        server.verify();
    }
}
