package com.sescam.radiodiagnostics.common.patients;

public record EmergencyContactDTO(
        String name,
        String relationship,
        String phone
) {
    public EmergencyContactDTO {
        name = name == null ? "" : name.trim();
        relationship = relationship == null ? "" : relationship.trim();
        phone = phone == null ? "" : phone.trim();
    }
}
