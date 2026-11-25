package com.sescam.radiodiagnostics.diagnostics.internal;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionLogDTO;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentDTO;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentSeverity;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentType;
import com.sescam.radiodiagnostics.common.diagnostics.ScheduledProcedureDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionRulesTest {

    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");
    private static final Instant START = Instant.parse("2026-11-24T09:00:00Z");
    private static final Instant END = Instant.parse("2026-11-24T09:30:00Z");
    private static final IncidentDTO INCIDENT = new IncidentDTO(IncidentType.ADVERSE_CONTRAST_REACTION,
            IncidentSeverity.MODERATE, "Urticaria after contrast", "Antihistamine", null);

    private final ExecutionRules rules = new ExecutionRules();

    private static ScheduledProcedureDTO procedure(ModalityType modality, boolean contrast, boolean consent) {
        return new ScheduledProcedureDTO("ORD-1", "PAT-1", "E", "Exam", modality, START, "ROOM", contrast, consent);
    }

    private static ExecutionLogDTO log(Instant start, Instant end, String room, String batch, Double volume,
                                       Double dlp, Double ctdi, Double mbq, boolean consent, boolean aborted,
                                       String reason, IncidentDTO... incidents) {
        return new ExecutionLogDTO("ORD-1", start, end, room, batch, volume, dlp, ctdi, mbq, consent, aborted, reason,
                Arrays.asList(incidents), "dr-1", "PHYSICIAN");
    }

    @Test
    void completeCtWithContrastIsValid() {
        assertThat(rules.invalidFields(log(START, END, "CT-1", "LOT-7", 80.0, 450.0, 12.5, null, true, false, null),
                procedure(ModalityType.CT, true, true), NOW)).isEmpty();
    }

    @Test
    void timesAndRoomAreMandatoryAndConsistent() {
        assertThat(rules.invalidFields(log(null, null, " ", null, null, null, null, null, false, false, null),
                procedure(ModalityType.X_RAY, false, false), NOW))
                .containsExactly("startedAt", "endedAt", "roomEquipmentId");
        assertThat(rules.invalidFields(log(END, START, "R", null, null, null, null, null, false, false, null),
                procedure(ModalityType.X_RAY, false, false), NOW)).containsExactly("endedAt");
        assertThat(rules.invalidFields(log(NOW.plusSeconds(60), NOW.plusSeconds(120), "R", null, null, null, null, null,
                false, false, null), procedure(ModalityType.X_RAY, false, false), NOW))
                .containsExactly("startedAt", "endedAt");
    }

    @Test
    void consentMustBeVerifiedWhenRequired() {
        assertThat(rules.invalidFields(log(START, END, "R", null, null, null, null, null, false, false, null),
                procedure(ModalityType.MRI, false, true), NOW)).containsExactly("informedConsentVerified");
    }

    @Test
    void contrastNeedsBatchAndCompletedContrastExamNeedsVolume() {
        assertThat(rules.invalidFields(log(START, END, "R", " ", 50.0, null, null, null, true, false, null),
                procedure(ModalityType.MRI, true, true), NOW)).containsExactly("contrastBatch");
        assertThat(rules.invalidFields(log(START, END, "R", null, null, null, null, null, true, false, null),
                procedure(ModalityType.MRI, true, true), NOW)).containsExactly("contrastVolumeMl");
    }

    @Test
    void doseFieldsDependOnTheModalityAndAreNeverNegative() {
        assertThat(rules.invalidFields(log(START, END, "R", null, null, null, 0.0, null, false, false, null),
                procedure(ModalityType.CT, false, false), NOW)).containsExactly("doseDlp", "doseCtdiVol");
        assertThat(rules.invalidFields(log(START, END, "R", null, null, null, null, null, false, false, null),
                procedure(ModalityType.NUCLEAR_MEDICINE, false, false), NOW)).containsExactly("radiotracerActivityMBq");
        assertThat(rules.invalidFields(log(START, END, "R", null, -1.0, -2.0, -3.0, -4.0, false, false, null),
                procedure(ModalityType.ULTRASOUND, false, false), NOW))
                .containsExactly("contrastVolumeMl", "doseDlp", "doseCtdiVol", "radiotracerActivityMBq");
    }

    @Test
    void abortedProcedureNeedsReasonAndIncidentButNotTheCompletionData() {
        assertThat(rules.invalidFields(log(START, END, "R", null, null, null, null, null, true, true, " "),
                procedure(ModalityType.CT, true, true), NOW)).containsExactly("abortReason", "incidents");
        assertThat(rules.invalidFields(log(START, END, "R", null, null, null, null, null, true, true, "Reaction",
                INCIDENT), procedure(ModalityType.CT, true, true), NOW)).isEmpty();
    }

    @Test
    void everyIncidentNeedsTypeSeverityAndDescription() {
        IncidentDTO empty = new IncidentDTO(null, null, " ", null, null);
        List<String> invalid = rules.invalidFields(log(START, END, "R", null, null, null, null, null, false, false,
                null, INCIDENT, empty), procedure(ModalityType.X_RAY, false, false), NOW);

        assertThat(invalid).containsExactly(
                "incidents[1].type", "incidents[1].severity", "incidents[1].description");
    }

    @Test
    void incidentTimeIsWithinTheExaminationAndNotInTheFuture() {
        IncidentDTO during = withTime(START.plusSeconds(600));
        IncidentDTO atStart = withTime(START);
        IncidentDTO atEnd = withTime(END);
        IncidentDTO before = withTime(START.minusSeconds(1));
        IncidentDTO after = withTime(END.plusSeconds(1));
        IncidentDTO future = withTime(NOW.plusSeconds(60));

        assertThat(rules.invalidFields(log(START, END, "R", null, null, null, null, null, false, false, null,
                INCIDENT, during, atStart, atEnd, before, after, future), procedure(ModalityType.X_RAY, false, false),
                NOW)).containsExactly("incidents[4].occurredAt", "incidents[5].occurredAt", "incidents[6].occurredAt");
    }

    @Test
    void incidentTimeWithInvalidStartOrEndIsOnlyCheckedAgainstNow() {
        assertThat(rules.invalidFields(log(null, null, "R", null, null, null, null, null, false, false, null,
                withTime(START), withTime(NOW.plusSeconds(1))), procedure(ModalityType.X_RAY, false, false), NOW))
                .containsExactly("startedAt", "endedAt", "incidents[1].occurredAt");
    }

    private static IncidentDTO withTime(Instant occurredAt) {
        return new IncidentDTO(IncidentType.EQUIPMENT_MALFUNCTION, IncidentSeverity.MILD, "Injector alarm", null,
                occurredAt);
    }

    @Test
    void aNullIncidentIsRejectedWhenTheLogIsBuilt() {
        org.assertj.core.api.Assertions.assertThatNullPointerException().isThrownBy(() ->
                log(START, END, "R", null, null, null, null, null, false, false, null, INCIDENT, null));
    }
}
