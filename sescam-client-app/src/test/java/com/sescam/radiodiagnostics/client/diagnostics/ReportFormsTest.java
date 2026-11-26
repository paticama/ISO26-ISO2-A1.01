package com.sescam.radiodiagnostics.client.diagnostics;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.diagnostics.EhrReportSyncStatus;
import com.sescam.radiodiagnostics.common.diagnostics.ReportDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ReportStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportFormsTest {

    @Test
    void reportFormValidatesDraftAndFinalizeAndPopulatesFromDto() {
        ReportForm empty = new ReportForm();
        assertThat(empty.validateDraft()).containsOnlyKeys("executionId");
        assertThat(empty.validateFinalize()).containsOnlyKeys("executionId", "findings", "conclusion", "digitalSignature");

        empty.setExecutionId(" EXE-1 ");
        empty.setFindings("x".repeat(ReportForm.MAX_TEXT_LENGTH + 1));
        empty.setConclusion("y".repeat(ReportForm.MAX_TEXT_LENGTH + 1));
        assertThat(empty.validateDraft()).containsOnlyKeys("findings", "conclusion");

        ReportDTO dto = new ReportDTO("REP-1", "EXE-1", "ORD-1", "PAT-1", "1.2.3", "CT", ModalityType.CT,
                "Findings", "Conclusion", true, true, ReportStatus.DRAFT, "dr-1", null, null,
                EhrReportSyncStatus.NOT_SYNCED, null, 2L, Instant.now(), Instant.now(), List.of());

        ReportForm form = ReportForm.fromReport(dto);
        assertThat(form.validateDraft()).isEmpty();
        assertThat(form.validateFinalize()).containsOnlyKeys("digitalSignature");

        form.setDigitalSignature(" SIG-DR1 ");
        assertThat(form.validateFinalize()).isEmpty();

        DiagnosticReportApiClient.DraftPayload draft = form.toDraftPayload();
        assertThat(draft.executionId()).isEqualTo("EXE-1");
        assertThat(draft.criticalAlert()).isTrue();
        assertThat(draft.followUpRecommended()).isTrue();
        assertThat(draft.expectedVersion()).isEqualTo(2L);

        DiagnosticReportApiClient.FinalizePayload finalize = form.toFinalizePayload();
        assertThat(finalize.digitalSignature()).isEqualTo("SIG-DR1");
        assertThat(form.getFindings()).isEqualTo("Findings");
        assertThat(form.getConclusion()).isEqualTo("Conclusion");
        assertThat(form.getDigitalSignature()).isEqualTo(" SIG-DR1 ");
        assertThat(form.getExpectedVersion()).isEqualTo(2L);
    }

    @Test
    void addendumFormValidatesMandatoryFieldsAndLength() {
        AddendumForm form = new AddendumForm();
        assertThat(form.validate()).containsOnlyKeys("reportId", "reason", "addendumContent", "digitalSignature");

        form.setReportId(" REP-1 ");
        form.setReason(" Clarification ");
        form.setAddendumContent("x".repeat(AddendumForm.MAX_CONTENT_LENGTH + 1));
        form.setDigitalSignature(" SIG-2 ");
        form.setExpectedVersion(3L);
        assertThat(form.validate()).containsOnlyKeys("addendumContent");

        form.setAddendumContent(" No residual nodule ");
        assertThat(form.validate()).isEmpty();
        DiagnosticReportApiClient.AddendumPayload payload = form.toPayload();
        assertThat(payload.reason()).isEqualTo("Clarification");
        assertThat(payload.addendumContent()).isEqualTo("No residual nodule");
        assertThat(payload.digitalSignature()).isEqualTo("SIG-2");
        assertThat(payload.expectedVersion()).isEqualTo(3L);
        assertThat(form.getReportId()).isEqualTo(" REP-1 ");
        assertThat(form.getReason()).isEqualTo(" Clarification ");
        assertThat(form.getAddendumContent()).isEqualTo(" No residual nodule ");
        assertThat(form.getDigitalSignature()).isEqualTo(" SIG-2 ");
        assertThat(form.getExpectedVersion()).isEqualTo(3L);
    }
}
