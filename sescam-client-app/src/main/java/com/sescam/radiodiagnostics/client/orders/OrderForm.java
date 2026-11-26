package com.sescam.radiodiagnostics.client.orders;

import com.sescam.radiodiagnostics.common.orders.PriorityLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prescription form of the UC-PHY-01 view. It checks the mandatory data before calling the server (A4a), so a
 * missing indication, justification or priority never leaves the client.
 */
public class OrderForm {

    static final int MAX_TEXT_LENGTH = 2000;

    private String patientId;
    private String examTypeCode;
    private String clinicalIndication;
    private String clinicalJustification;
    private PriorityLevel priority;
    private String overrideJustification;
    private List<String> acknowledgedConstraintIds = new ArrayList<>();

    /** Field name → error message; empty when the form is valid. */
    public Map<String, String> validate() {
        Map<String, String> errors = new LinkedHashMap<>();
        if (isBlank(patientId)) {
            errors.put("patientId", "Enter the patient.");
        }
        if (isBlank(examTypeCode)) {
            errors.put("examTypeCode", "Select the examination type.");
        }
        checkText(errors, "clinicalIndication", clinicalIndication, "The clinical indication is mandatory.");
        checkText(errors, "clinicalJustification", clinicalJustification, "The clinical justification is mandatory.");
        if (priority == null) {
            errors.put("priority", "Select the priority level.");
        }
        if (!isBlank(overrideJustification) && overrideJustification.trim().length() > MAX_TEXT_LENGTH) {
            errors.put("overrideJustification", "Use at most " + MAX_TEXT_LENGTH + " characters.");
        }
        return errors;
    }

    private static void checkText(Map<String, String> errors, String field, String value, String missing) {
        if (isBlank(value)) {
            errors.put(field, missing);
        } else if (value.trim().length() > MAX_TEXT_LENGTH) {
            errors.put(field, "Use at most " + MAX_TEXT_LENGTH + " characters.");
        }
    }

    /** Payload for the server. Call only on a valid form. */
    public OrderApiClient.OrderPayload toPayload() {
        return new OrderApiClient.OrderPayload(patientId.trim(), examTypeCode.trim(), clinicalIndication.trim(),
                clinicalJustification.trim(), priority.name(),
                isBlank(overrideJustification) ? null : overrideJustification.trim(),
                acknowledgedConstraintIds == null ? List.of() : List.copyOf(acknowledgedConstraintIds));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getExamTypeCode() {
        return examTypeCode;
    }

    public void setExamTypeCode(String examTypeCode) {
        this.examTypeCode = examTypeCode;
    }

    public String getClinicalIndication() {
        return clinicalIndication;
    }

    public void setClinicalIndication(String clinicalIndication) {
        this.clinicalIndication = clinicalIndication;
    }

    public String getClinicalJustification() {
        return clinicalJustification;
    }

    public void setClinicalJustification(String clinicalJustification) {
        this.clinicalJustification = clinicalJustification;
    }

    public PriorityLevel getPriority() {
        return priority;
    }

    public void setPriority(PriorityLevel priority) {
        this.priority = priority;
    }

    public String getOverrideJustification() {
        return overrideJustification;
    }

    public void setOverrideJustification(String overrideJustification) {
        this.overrideJustification = overrideJustification;
    }

    public List<String> getAcknowledgedConstraintIds() {
        return acknowledgedConstraintIds;
    }

    public void setAcknowledgedConstraintIds(List<String> acknowledgedConstraintIds) {
        this.acknowledgedConstraintIds = acknowledgedConstraintIds == null ? new ArrayList<>() : acknowledgedConstraintIds;
    }
}
