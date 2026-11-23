package com.sescam.radiodiagnostics.common.patients;

import java.util.Objects;

public record AddressDTO(
        String street,
        String postalCode,
        String city,
        String province
) {
    public AddressDTO {
        street = street == null ? "" : street.trim();
        postalCode = postalCode == null ? "" : postalCode.trim();
        city = city == null ? "" : city.trim();
        province = province == null ? "" : province.trim();
    }
}
