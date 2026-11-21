package com.sescam.radiodiagnostics.client.catalogue;

import com.sescam.radiodiagnostics.common.catalogue.ActivationMode;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationParameterDTO;
import com.sescam.radiodiagnostics.common.catalogue.ParameterValueType;
import com.sescam.radiodiagnostics.common.catalogue.ValidationResultDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationFormValidatorTest {

    private final ConfigurationFormValidator validator = new ConfigurationFormValidator();

    private ConfigurationParameterDTO param(String key, ParameterValueType type,
                                            Double min, Double max, List<String> allowed) {
        return new ConfigurationParameterDTO(
                key, key, "CAT", "desc", "10", type, min, max, allowed,
                ActivationMode.IMMEDIATE, 1L, "system", Instant.now()
        );
    }

    @Test
    @DisplayName("Null parameter or blank proposed value is rejected locally")
    void nullParameterOrBlankValueRejected() {
        assertFalse(validator.validate(null, "10").valid());
        ConfigurationParameterDTO p = param("exam.duration", ParameterValueType.INTEGER, 5.0, 60.0, List.of());
        assertFalse(validator.validate(p, null).valid());
        assertFalse(validator.validate(p, "   ").valid());
    }

    @Test
    @DisplayName("Integer, Decimal, Boolean, Enum, and String validations enforce type and bounds")
    void allValueTypesValidatedCorrectly() {
        ConfigurationParameterDTO intParam = param("triage.sla", ParameterValueType.INTEGER, 1.0, 48.0, List.of());
        assertTrue(validator.validate(intParam, "12").valid());
        assertFalse(validator.validate(intParam, "0").valid());
        assertFalse(validator.validate(intParam, "100").valid());
        assertFalse(validator.validate(intParam, "abc").valid());

        ConfigurationParameterDTO decParam = param("dose.max", ParameterValueType.DECIMAL, 0.5, 5.0, List.of());
        assertTrue(validator.validate(decParam, "2.5").valid());
        assertFalse(validator.validate(decParam, "0.1").valid());
        assertFalse(validator.validate(decParam, "9.9").valid());
        assertFalse(validator.validate(decParam, "NaN").valid());
        assertFalse(validator.validate(decParam, "xyz").valid());

        ConfigurationParameterDTO boolParam = param("quiet.hours", ParameterValueType.BOOLEAN, null, null, List.of());
        assertTrue(validator.validate(boolParam, "true").valid());
        assertTrue(validator.validate(boolParam, "FALSE").valid());
        assertFalse(validator.validate(boolParam, "yes").valid());

        ConfigurationParameterDTO enumParam = param("pacs.mode", ParameterValueType.ENUM, null, null, List.of("WADO-RS", "C-MOVE"));
        assertTrue(validator.validate(enumParam, "WADO-RS").valid());
        assertFalse(validator.validate(enumParam, "FTP").valid());

        ConfigurationParameterDTO strParam = param("hospital.name", ParameterValueType.STRING, null, null, List.of());
        ValidationResultDTO ok = validator.validate(strParam, "Hospital General");
        assertTrue(ok.valid());
        assertEquals("hospital.name", ok.parameterKey());
    }
}
