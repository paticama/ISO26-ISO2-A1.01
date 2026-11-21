package com.sescam.radiodiagnostics.common.catalogue;

import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CatalogueContractsTest {

    @Test
    void validationResultFactoryMethodsWork() {
        ValidationResultDTO ok = ValidationResultDTO.ok("exam.default.duration");
        assertTrue(ok.valid());
        assertEquals("exam.default.duration", ok.parameterKey());
        assertNull(ok.errorMessage());

        ValidationResultDTO invalid = ValidationResultDTO.invalid("exam.default.duration", "Must be > 0");
        assertFalse(invalid.valid());
        assertEquals("Must be > 0", invalid.errorMessage());
    }

    @Test
    void domainExceptionsExposeStructuredContext() {
        ConfigurationNotFoundException notFound = new ConfigurationNotFoundException("missing.key");
        assertEquals("missing.key", notFound.getParameterKey());

        UnauthorizedConfigurationAccessException unauth =
                new UnauthorizedConfigurationAccessException("user-1", "Denied");
        assertEquals("user-1", unauth.getActorId());
        assertEquals("Denied", unauth.getMessage());

        InvalidConfigurationValueException invalid =
                new InvalidConfigurationValueException("triage.sla", "Out of range");
        assertEquals("triage.sla", invalid.getParameterKey());

        ConcurrentConfigurationChangeException conflict =
                new ConcurrentConfigurationChangeException("triage.sla", 1L, 2L, "4");
        assertEquals("triage.sla", conflict.getParameterKey());
        assertEquals(1L, conflict.getExpectedVersion());
        assertEquals(2L, conflict.getActualVersion());
        assertEquals("4", conflict.getCurrentStoredValue());

        RuntimeException cause = new RuntimeException("DB down");
        ConfigurationSaveException saveEx = new ConfigurationSaveException("triage.sla", "2", cause);
        assertEquals("triage.sla", saveEx.getParameterKey());
        assertEquals("2", saveEx.getPreviousValue());
        assertSame(cause, saveEx.getCause());
    }

    @Test
    void dtosAndEnumsPreserveValues() {
        Instant now = Instant.now();
        ConfigurationParameterDTO param = new ConfigurationParameterDTO(
                "booking.cancellation.min.hours", "Minimum Cancellation Window", "POLICIES",
                "Minimum hours before exam to cancel", "24", ParameterValueType.INTEGER,
                1.0, 168.0, List.of(), ActivationMode.IMMEDIATE, 1L, "admin", now
        );
        assertEquals("booking.cancellation.min.hours", param.key());
        assertEquals(ActivationMode.IMMEDIATE, param.activationMode());
        assertEquals(ParameterValueType.INTEGER, param.valueType());

        UpdateParameterRequestDTO req = new UpdateParameterRequestDTO(
                "booking.cancellation.min.hours", "48", 1L, "admin-1", "ADMINISTRATOR"
        );
        assertEquals("48", req.proposedValue());

        UpdateParameterResponseDTO resp = new UpdateParameterResponseDTO(
                "booking.cancellation.min.hours", "24", "48", 2L,
                ActivationStatus.ACTIVE, "Updated", "admin-1", now
        );
        assertEquals(ActivationStatus.ACTIVE, resp.activationStatus());

        AuditEventDTO audit = new AuditEventDTO(
                "CONFIG_UPDATED", "admin-1", "booking.cancellation.min.hours",
                "24", "48", "ACTIVE", now
        );
        assertEquals("CONFIG_UPDATED", audit.action());
        assertEquals(3, ActivationMode.values().length);
        assertEquals(3, ActivationStatus.values().length);
        assertEquals(5, ParameterValueType.values().length);
    }
}
