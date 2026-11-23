package com.sescam.radiodiagnostics.common.patients;

public record PatientSearchCriteriaDTO(
        String query,
        PatientStatus status,
        int page,
        int size
) {
    public PatientSearchCriteriaDTO {
        query = query == null ? "" : query.trim();
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
    }

    public static PatientSearchCriteriaDTO ofQuery(String query) {
        return new PatientSearchCriteriaDTO(query, null, 0, 50);
    }
}
