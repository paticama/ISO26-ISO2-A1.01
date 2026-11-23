package com.sescam.radiodiagnostics.patients.domain;

public record Address(
        String street,
        String postalCode,
        String city,
        String province
) {
    public Address {
        street = street == null ? "" : street.trim();
        postalCode = postalCode == null ? "" : postalCode.trim();
        city = city == null ? "" : city.trim();
        province = province == null ? "" : province.trim();
    }
}
