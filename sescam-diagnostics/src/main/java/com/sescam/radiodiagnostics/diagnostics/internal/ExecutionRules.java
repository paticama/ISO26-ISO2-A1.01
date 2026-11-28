package com.sescam.radiodiagnostics.diagnostics.internal;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionLogDTO;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ScheduledProcedureDTO;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Data rules of an execution log (UC-PHY-05, FR-PHY-09/10/11/17):
 * <ul>
 *   <li>start, end and room/equipment are mandatory; the end is not before the start; neither is in the future;</li>
 *   <li>informed consent must be verified when the examination requires it (NFR-05);</li>
 *   <li>contrast administered needs its batch; a contrast examination that was completed needs the volume;</li>
 *   <li>a completed CT needs DLP and CTDIvol; a completed nuclear-medicine examination needs the activity;</li>
 *   <li>doses and volumes are not negative;</li>
 *   <li>2b: an aborted procedure needs a reason and at least one incident;</li>
 *   <li>2a: every incident needs type, severity and description; its time, if given, is within the examination
 *       and not in the future.</li>
 * </ul>
 */
public class ExecutionRules {

    /** Names of the invalid fields, in a stable order; empty when the log is valid. */
    public List<String> invalidFields(ExecutionLogDTO log, ScheduledProcedureDTO procedure, Instant now) {
        Set<String> invalid = new LinkedHashSet<>();

        if (log.startedAt() == null || log.startedAt().isAfter(now)) {
            invalid.add("startedAt");
        }
        if (log.endedAt() == null || log.endedAt().isAfter(now)
                || (log.startedAt() != null && log.endedAt().isBefore(log.startedAt()))) {
            invalid.add("endedAt");
        }
        if (isBlank(log.roomEquipmentId())) {
            invalid.add("roomEquipmentId");
        }
        if (procedure.requiresInformedConsent() && !log.informedConsentVerified()) {
            invalid.add("informedConsentVerified");
        }

        checkNotNegative(invalid, "contrastVolumeMl", log.contrastVolumeMl());
        checkNotNegative(invalid, "doseDlp", log.doseDlp());
        checkNotNegative(invalid, "doseCtdiVol", log.doseCtdiVol());
        checkNotNegative(invalid, "radiotracerActivityMBq", log.radiotracerActivityMBq());

        boolean contrastGiven = log.contrastVolumeMl() != null && log.contrastVolumeMl() > 0;
        if (contrastGiven && isBlank(log.contrastBatch())) {
            invalid.add("contrastBatch");
        }

        if (!log.aborted()) {
            if (procedure.requiresContrast() && !contrastGiven) {
                invalid.add("contrastVolumeMl");
            }
            if (procedure.modality() == ModalityType.CT) {
                requirePositive(invalid, "doseDlp", log.doseDlp());
                requirePositive(invalid, "doseCtdiVol", log.doseCtdiVol());
            }
            if (procedure.modality() == ModalityType.NUCLEAR_MEDICINE) {
                requirePositive(invalid, "radiotracerActivityMBq", log.radiotracerActivityMBq());
            }
        } else {
            if (isBlank(log.abortReason())) {
                invalid.add("abortReason");
            }
            if (log.incidents().isEmpty()) {
                invalid.add("incidents");
            }
        }

        for (int i = 0; i < log.incidents().size(); i++) {
            IncidentDTO incident = log.incidents().get(i);
            String prefix = "incidents[" + i + "].";
            // null entries cannot reach here: ExecutionLogDTO rejects them when it is built
            if (incident.type() == null) {
                invalid.add(prefix + "type");
            }
            if (incident.severity() == null) {
                invalid.add(prefix + "severity");
            }
            if (isBlank(incident.description())) {
                invalid.add(prefix + "description");
            }
            if (!occurredDuringExamination(incident.occurredAt(), log, now)) {
                invalid.add(prefix + "occurredAt");
            }
        }
        return new ArrayList<>(invalid);
    }

    /**
     * An incident time, when given, is not in the future and lies within the examination (start to end, or to the
     * abort time in 2b). {@code null} means the end of the execution and is always valid. When start or end is itself
     * invalid only the future check applies, so the error is reported on that field.
     */
    private static boolean occurredDuringExamination(Instant occurredAt, ExecutionLogDTO log, Instant now) {
        if (occurredAt == null) {
            return true;
        }
        if (occurredAt.isAfter(now)) {
            return false;
        }
        boolean afterStart = log.startedAt() == null || !occurredAt.isBefore(log.startedAt());
        boolean beforeEnd = log.endedAt() == null || !occurredAt.isAfter(log.endedAt());
        return afterStart && beforeEnd;
    }

    private static void checkNotNegative(Set<String> invalid, String field, Double value) {
        if (value != null && value < 0) {
            invalid.add(field);
        }
    }

    private static void requirePositive(Set<String> invalid, String field, Double value) {
        if (value == null || value <= 0) {
            invalid.add(field);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
