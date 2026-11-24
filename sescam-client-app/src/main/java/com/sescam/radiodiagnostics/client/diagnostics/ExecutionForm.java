package com.sescam.radiodiagnostics.client.diagnostics;

import com.sescam.radiodiagnostics.common.diagnostics.IncidentDTO;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentSeverity;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentType;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Execution form of the UC-PHY-05 view. It checks what does not depend on the procedure (mandatory fields, times,
 * negative values, incident and abort data) before calling the server. The rules that depend on the procedure
 * (consent required, dose per modality) are checked by the server and come back as field errors.
 *
 * <p>The view documents one incident per submission; the server API accepts a list.
 */
public class ExecutionForm {

    static final int MAX_TEXT_LENGTH = 2000;

    private String orderId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String roomEquipmentId;
    private String contrastBatch;
    private Double contrastVolumeMl;
    private Double doseDlp;
    private Double doseCtdiVol;
    private Double radiotracerActivityMBq;
    private boolean informedConsentVerified;
    private IncidentType incidentType;
    private IncidentSeverity incidentSeverity;
    private String incidentDescription;
    private String incidentTreatment;
    private boolean aborted;
    private String abortReason;

    /** Field name → error message; empty when the form is valid. */
    public Map<String, String> validate() {
        Map<String, String> errors = new LinkedHashMap<>();
        if (isBlank(orderId)) {
            errors.put("orderId", "Select the procedure.");
        }
        if (startedAt == null) {
            errors.put("startedAt", "Enter the actual start.");
        }
        if (endedAt == null) {
            errors.put("endedAt", "Enter the actual end.");
        } else if (startedAt != null && endedAt.isBefore(startedAt)) {
            errors.put("endedAt", "The end must be after the start.");
        }
        if (isBlank(roomEquipmentId)) {
            errors.put("roomEquipmentId", "Enter the room or equipment used.");
        }
        notNegative(errors, "contrastVolumeMl", contrastVolumeMl);
        notNegative(errors, "doseDlp", doseDlp);
        notNegative(errors, "doseCtdiVol", doseCtdiVol);
        notNegative(errors, "radiotracerActivityMBq", radiotracerActivityMBq);
        if (contrastVolumeMl != null && contrastVolumeMl > 0 && isBlank(contrastBatch)) {
            errors.put("contrastBatch", "Enter the batch of the contrast administered.");
        }
        if (hasIncident() || aborted) {
            if (incidentType == null) {
                errors.put("incidentType", "Select the incident type.");
            }
            if (incidentSeverity == null) {
                errors.put("incidentSeverity", "Select the severity.");
            }
            if (isBlank(incidentDescription)) {
                errors.put("incidentDescription", "Describe the incident.");
            } else if (incidentDescription.trim().length() > MAX_TEXT_LENGTH) {
                errors.put("incidentDescription", "Use at most " + MAX_TEXT_LENGTH + " characters.");
            }
        }
        if (aborted && isBlank(abortReason)) {
            errors.put("abortReason", "Give the reason the procedure was aborted.");
        }
        return errors;
    }

    /** True when any incident field was filled in (2a). */
    public boolean hasIncident() {
        return incidentType != null || incidentSeverity != null || !isBlank(incidentDescription)
                || !isBlank(incidentTreatment);
    }

    /** Payload for the server, with local times read in {@code zone}. Call only on a valid form. */
    public ExecutionApiClient.ExecutionPayload toPayload(ZoneId zone) {
        List<IncidentDTO> incidents = hasIncident()
                ? List.of(new IncidentDTO(incidentType, incidentSeverity, incidentDescription.trim(),
                blankToNull(incidentTreatment), null))
                : List.of();
        return new ExecutionApiClient.ExecutionPayload(orderId.trim(), startedAt.atZone(zone).toInstant(),
                endedAt.atZone(zone).toInstant(), roomEquipmentId.trim(), blankToNull(contrastBatch),
                contrastVolumeMl, doseDlp, doseCtdiVol, radiotracerActivityMBq, informedConsentVerified, aborted,
                aborted ? abortReason.trim() : null, incidents);
    }

    private static void notNegative(Map<String, String> errors, String field, Double value) {
        if (value != null && value < 0) {
            errors.put(field, "The value cannot be negative.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public String getRoomEquipmentId() {
        return roomEquipmentId;
    }

    public void setRoomEquipmentId(String roomEquipmentId) {
        this.roomEquipmentId = roomEquipmentId;
    }

    public String getContrastBatch() {
        return contrastBatch;
    }

    public void setContrastBatch(String contrastBatch) {
        this.contrastBatch = contrastBatch;
    }

    public Double getContrastVolumeMl() {
        return contrastVolumeMl;
    }

    public void setContrastVolumeMl(Double contrastVolumeMl) {
        this.contrastVolumeMl = contrastVolumeMl;
    }

    public Double getDoseDlp() {
        return doseDlp;
    }

    public void setDoseDlp(Double doseDlp) {
        this.doseDlp = doseDlp;
    }

    public Double getDoseCtdiVol() {
        return doseCtdiVol;
    }

    public void setDoseCtdiVol(Double doseCtdiVol) {
        this.doseCtdiVol = doseCtdiVol;
    }

    public Double getRadiotracerActivityMBq() {
        return radiotracerActivityMBq;
    }

    public void setRadiotracerActivityMBq(Double radiotracerActivityMBq) {
        this.radiotracerActivityMBq = radiotracerActivityMBq;
    }

    public boolean isInformedConsentVerified() {
        return informedConsentVerified;
    }

    public void setInformedConsentVerified(boolean informedConsentVerified) {
        this.informedConsentVerified = informedConsentVerified;
    }

    public IncidentType getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(IncidentType incidentType) {
        this.incidentType = incidentType;
    }

    public IncidentSeverity getIncidentSeverity() {
        return incidentSeverity;
    }

    public void setIncidentSeverity(IncidentSeverity incidentSeverity) {
        this.incidentSeverity = incidentSeverity;
    }

    public String getIncidentDescription() {
        return incidentDescription;
    }

    public void setIncidentDescription(String incidentDescription) {
        this.incidentDescription = incidentDescription;
    }

    public String getIncidentTreatment() {
        return incidentTreatment;
    }

    public void setIncidentTreatment(String incidentTreatment) {
        this.incidentTreatment = incidentTreatment;
    }

    public boolean isAborted() {
        return aborted;
    }

    public void setAborted(boolean aborted) {
        this.aborted = aborted;
    }

    public String getAbortReason() {
        return abortReason;
    }

    public void setAbortReason(String abortReason) {
        this.abortReason = abortReason;
    }
}
