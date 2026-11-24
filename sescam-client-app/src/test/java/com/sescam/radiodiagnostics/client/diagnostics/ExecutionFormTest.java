package com.sescam.radiodiagnostics.client.diagnostics;

import com.sescam.radiodiagnostics.common.diagnostics.IncidentSeverity;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionFormTest {

    private static ExecutionForm valid() {
        ExecutionForm form = new ExecutionForm();
        form.setOrderId(" ORD-DEMO-CT ");
        form.setStartedAt(LocalDateTime.of(2026, 11, 24, 10, 5));
        form.setEndedAt(LocalDateTime.of(2026, 11, 24, 10, 35));
        form.setRoomEquipmentId(" CT-ROOM-1 ");
        form.setContrastBatch(" IOP-117 ");
        form.setContrastVolumeMl(90.0);
        form.setDoseDlp(520.0);
        form.setDoseCtdiVol(14.2);
        form.setInformedConsentVerified(true);
        return form;
    }

    @Test
    void mainFlowFormIsValidAndBuildsThePayloadInTheGivenZone() {
        ExecutionForm form = valid();

        assertThat(form.validate()).isEmpty();
        assertThat(form.hasIncident()).isFalse();
        ExecutionApiClient.ExecutionPayload payload = form.toPayload(ZoneId.of("Europe/Madrid"));
        assertThat(payload.orderId()).isEqualTo("ORD-DEMO-CT");
        assertThat(payload.startedAt()).isEqualTo(Instant.parse("2026-11-24T09:05:00Z"));
        assertThat(payload.endedAt()).isEqualTo(Instant.parse("2026-11-24T09:35:00Z"));
        assertThat(payload.roomEquipmentId()).isEqualTo("CT-ROOM-1");
        assertThat(payload.contrastBatch()).isEqualTo("IOP-117");
        assertThat(payload.doseDlp()).isEqualTo(520.0);
        assertThat(payload.informedConsentVerified()).isTrue();
        assertThat(payload.aborted()).isFalse();
        assertThat(payload.abortReason()).isNull();
        assertThat(payload.incidents()).isEmpty();
    }

    @Test
    void mandatoryFieldsAndTimesAreChecked() {
        assertThat(new ExecutionForm().validate()).containsOnlyKeys("orderId", "startedAt", "endedAt", "roomEquipmentId");

        ExecutionForm form = valid();
        form.setEndedAt(LocalDateTime.of(2026, 11, 24, 9, 0));
        assertThat(form.validate()).containsOnlyKeys("endedAt");
    }

    @Test
    void negativeValuesAndContrastWithoutBatchAreRejected() {
        ExecutionForm form = valid();
        form.setContrastBatch(" ");
        form.setDoseDlp(-1.0);
        form.setDoseCtdiVol(-1.0);
        form.setRadiotracerActivityMBq(-1.0);

        assertThat(form.validate()).containsOnlyKeys("contrastBatch", "doseDlp", "doseCtdiVol", "radiotracerActivityMBq");

        form = valid();
        form.setContrastVolumeMl(-5.0);
        assertThat(form.validate()).containsOnlyKeys("contrastVolumeMl");
    }

    @Test
    void alternative2aIncidentNeedsTypeSeverityAndDescriptionAndIsSent() {
        ExecutionForm form = valid();
        form.setIncidentTreatment("Antihistamine");
        assertThat(form.hasIncident()).isTrue();
        assertThat(form.validate()).containsOnlyKeys("incidentType", "incidentSeverity", "incidentDescription");

        form.setIncidentType(IncidentType.ADVERSE_CONTRAST_REACTION);
        form.setIncidentSeverity(IncidentSeverity.MODERATE);
        form.setIncidentDescription(" Urticaria ");
        assertThat(form.validate()).isEmpty();
        assertThat(form.toPayload(ZoneId.of("UTC")).incidents()).singleElement().satisfies(i -> {
            assertThat(i.description()).isEqualTo("Urticaria");
            assertThat(i.treatmentGiven()).isEqualTo("Antihistamine");
            assertThat(i.occurredAt()).isNull();
        });

        form.setIncidentDescription("x".repeat(ExecutionForm.MAX_TEXT_LENGTH + 1));
        assertThat(form.validate()).containsOnlyKeys("incidentDescription");
    }

    @Test
    void alternative2bAbortNeedsReasonAndIncident() {
        ExecutionForm form = valid();
        form.setAborted(true);
        assertThat(form.validate()).containsOnlyKeys("incidentType", "incidentSeverity", "incidentDescription", "abortReason");

        form.setIncidentType(IncidentType.ANAPHYLACTOID_EVENT);
        form.setIncidentSeverity(IncidentSeverity.SEVERE);
        form.setIncidentDescription("Anaphylactoid reaction");
        form.setAbortReason(" Severe reaction ");
        assertThat(form.validate()).isEmpty();
        ExecutionApiClient.ExecutionPayload payload = form.toPayload(ZoneId.of("UTC"));
        assertThat(payload.aborted()).isTrue();
        assertThat(payload.abortReason()).isEqualTo("Severe reaction");
        assertThat(payload.incidents()).hasSize(1);
    }

    @Test
    void gettersReturnWhatWasSet() {
        ExecutionForm form = valid();
        form.setAbortReason("r");
        form.setRadiotracerActivityMBq(500.0);
        assertThat(form.getOrderId()).isEqualTo(" ORD-DEMO-CT ");
        assertThat(form.getStartedAt()).isNotNull();
        assertThat(form.getEndedAt()).isNotNull();
        assertThat(form.getRoomEquipmentId()).isEqualTo(" CT-ROOM-1 ");
        assertThat(form.getContrastBatch()).isEqualTo(" IOP-117 ");
        assertThat(form.getContrastVolumeMl()).isEqualTo(90.0);
        assertThat(form.getDoseDlp()).isEqualTo(520.0);
        assertThat(form.getDoseCtdiVol()).isEqualTo(14.2);
        assertThat(form.getRadiotracerActivityMBq()).isEqualTo(500.0);
        assertThat(form.isInformedConsentVerified()).isTrue();
        assertThat(form.getIncidentType()).isNull();
        assertThat(form.getIncidentSeverity()).isNull();
        assertThat(form.getIncidentDescription()).isNull();
        assertThat(form.getIncidentTreatment()).isNull();
        assertThat(form.isAborted()).isFalse();
        assertThat(form.getAbortReason()).isEqualTo("r");
    }
}
