package com.sescam.radiodiagnostics.patients.domain;

public record EmergencyContact(
        String name,
        String relationship,
        String phone
) {
    public EmergencyContact {
        name = name == null ? "" : name.trim();
        relationship = relationship == null ? "" : relationship.trim();
        phone = phone == null ? "" : phone.trim();
    }
}
