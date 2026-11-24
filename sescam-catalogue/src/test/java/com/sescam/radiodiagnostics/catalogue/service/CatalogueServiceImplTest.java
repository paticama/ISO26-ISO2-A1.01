package com.sescam.radiodiagnostics.catalogue.service;

import com.sescam.radiodiagnostics.catalogue.domain.ConfigurationParameter;
import com.sescam.radiodiagnostics.catalogue.internal.DefaultAccessControlService;
import com.sescam.radiodiagnostics.catalogue.internal.NoOpAuditService;
import com.sescam.radiodiagnostics.catalogue.repository.ConfigurationParameterRepository;
import com.sescam.radiodiagnostics.catalogue.repository.InMemoryConfigurationParameterRepository;
import com.sescam.radiodiagnostics.common.catalogue.ActivationMode;
import com.sescam.radiodiagnostics.common.catalogue.ActivationStatus;
import com.sescam.radiodiagnostics.common.catalogue.ConcurrentConfigurationChangeException;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationNotFoundException;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationParameterDTO;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationSaveException;
import com.sescam.radiodiagnostics.common.catalogue.InvalidConfigurationValueException;
import com.sescam.radiodiagnostics.common.catalogue.ParameterValueType;
import com.sescam.radiodiagnostics.common.catalogue.UnauthorizedConfigurationAccessException;
import com.sescam.radiodiagnostics.common.catalogue.UpdateParameterRequestDTO;
import com.sescam.radiodiagnostics.common.catalogue.UpdateParameterResponseDTO;
import com.sescam.radiodiagnostics.common.catalogue.ValidationResultDTO;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CatalogueServiceImplTest {

    private static final String ADMIN_ID = "admin-victoria";
    private static final String ADMIN_ROLE = "ADMINISTRATOR";
    private static final Instant FIXED_TIME = Instant.parse("2026-11-20T19:00:00Z");

    private InMemoryConfigurationParameterRepository repository;
    private DefaultAccessControlService accessControlService;
    private NoOpAuditService auditService;
    private CatalogueServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryConfigurationParameterRepository();
        accessControlService = new DefaultAccessControlService();
        auditService = new NoOpAuditService();
        Clock fixedClock = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);
        service = new CatalogueServiceImpl(repository, accessControlService, auditService, fixedClock);
    }

    @Test
    @DisplayName("AC1: Authorized administrator can view all system configuration parameters and current values")
    void authorizedAdminCanViewAllParameters() {
        List<ConfigurationParameterDTO> params = service.getAllParameters(ADMIN_ID, ADMIN_ROLE);

        assertFalse(params.isEmpty());
        assertTrue(params.stream().anyMatch(p -> p.key().equals("booking.cancellation.min.hours") && p.currentValue().equals("24")));
        assertTrue(params.stream().anyMatch(p -> p.key().equals("system.pacs.transfer.mode") && p.currentValue().equals("WADO-RS")));

        ConfigurationParameterDTO single = service.getParameter("triage.urgent.sla.hours", ADMIN_ID, ADMIN_ROLE);
        assertEquals("4", single.currentValue());
        assertEquals(1L, single.version());
    }

    @Test
    @DisplayName("AC2 & AC5 & AC7: Valid immediate parameter update is saved, reports ACTIVE status, and records audit event")
    void validImmediateUpdateSavesAndAudits() {
        UpdateParameterRequestDTO request = new UpdateParameterRequestDTO(
                "booking.cancellation.min.hours", "48", 1L, ADMIN_ID, ADMIN_ROLE
        );

        UpdateParameterResponseDTO response = service.updateParameter(request);

        assertEquals("booking.cancellation.min.hours", response.parameterKey());
        assertEquals("24", response.previousValue());
        assertEquals("48", response.currentValue());
        assertEquals(2L, response.newVersion());
        assertEquals(ActivationStatus.ACTIVE, response.activationStatus());
        assertTrue(response.message().contains("immediately"));
        assertEquals(ADMIN_ID, response.updatedBy());
        assertEquals(FIXED_TIME, response.updatedAt());

        // Persisted state verified
        ConfigurationParameterDTO persisted = service.getParameter("booking.cancellation.min.hours", ADMIN_ID, ADMIN_ROLE);
        assertEquals("48", persisted.currentValue());
        assertEquals(2L, persisted.version());

        // Audit attribution verified (NFR-11)
        assertEquals(1, auditService.getRecordedEvents().size());
        AuditEventDTO audit = auditService.getRecordedEvents().get(0);
        assertEquals("CONFIGURATION_PARAMETER_UPDATED", audit.action());
        assertEquals(ADMIN_ID, audit.actorId());
        assertEquals("24", audit.previousValue());
        assertEquals("48", audit.newValue());
    }

    @Test
    @DisplayName("AC5: Reload and restart activation modes report PENDING_RELOAD and PENDING_RESTART")
    void activationStatusesReportedCorrectly() {
        UpdateParameterResponseDTO reloadResp = service.updateParameter(new UpdateParameterRequestDTO(
                "notification.max.retries", "5", 1L, ADMIN_ID, ADMIN_ROLE
        ));
        assertEquals(ActivationStatus.PENDING_RELOAD, reloadResp.activationStatus());
        assertTrue(reloadResp.message().contains("reload"));

        UpdateParameterResponseDTO restartResp = service.updateParameter(new UpdateParameterRequestDTO(
                "system.pacs.transfer.mode", "C-MOVE", 1L, ADMIN_ID, ADMIN_ROLE
        ));
        assertEquals(ActivationStatus.PENDING_RESTART, restartResp.activationStatus());
        assertTrue(restartResp.message().contains("restart"));
    }

    @Test
    @DisplayName("Alt A1 (AC4): Unauthorized user is denied read and write access without modifying configuration")
    void unauthorizedUserIsDenied() {
        assertThrows(UnauthorizedConfigurationAccessException.class,
                () -> service.getAllParameters("physician-1", "PHYSICIAN"));

        UpdateParameterRequestDTO request = new UpdateParameterRequestDTO(
                "triage.urgent.sla.hours", "2", 1L, "patient-99", "PATIENT"
        );
        assertThrows(UnauthorizedConfigurationAccessException.class,
                () -> service.updateParameter(request));

        // Blank credentials also rejected
        assertFalse(accessControlService.canManageConfiguration("", "ADMINISTRATOR"));
        assertFalse(accessControlService.canManageConfiguration("admin", ""));
        assertTrue(accessControlService.canManageConfiguration("admin", "system_administrator"));
        assertTrue(accessControlService.canManageConfiguration("admin", "ADMIN"));

        // Value unchanged and no audit recorded
        assertEquals("4", repository.findByKey("triage.urgent.sla.hours").orElseThrow().getCurrentValue());
        assertTrue(auditService.getRecordedEvents().isEmpty());
    }

    @Test
    @DisplayName("Alt A2 (AC3): Invalid values are rejected with actionable feedback and persisted value stays unchanged")
    void invalidValuesRejectedWithoutModifyingPersistedConfig() {
        // Out of range integer (below min & above max & non-numeric)
        ValidationResultDTO belowMin = service.validateParameterUpdate("triage.urgent.sla.hours", "0", ADMIN_ID, ADMIN_ROLE);
        assertFalse(belowMin.valid());
        assertTrue(belowMin.errorMessage().contains("minimum"));

        ValidationResultDTO aboveMax = service.validateParameterUpdate("triage.urgent.sla.hours", "999", ADMIN_ID, ADMIN_ROLE);
        assertFalse(aboveMax.valid());
        assertTrue(aboveMax.errorMessage().contains("maximum"));

        assertThrows(InvalidConfigurationValueException.class, () -> service.updateParameter(
                new UpdateParameterRequestDTO("triage.urgent.sla.hours", "not-a-number", 1L, ADMIN_ID, ADMIN_ROLE)
        ));

        // Invalid boolean
        assertThrows(InvalidConfigurationValueException.class, () -> service.updateParameter(
                new UpdateParameterRequestDTO("notification.quiet.hours.enabled", "maybe", 1L, ADMIN_ID, ADMIN_ROLE)
        ));

        // Invalid enum
        assertThrows(InvalidConfigurationValueException.class, () -> service.updateParameter(
                new UpdateParameterRequestDTO("system.pacs.transfer.mode", "FTP", 1L, ADMIN_ID, ADMIN_ROLE)
        ));

        // Blank value
        assertThrows(InvalidConfigurationValueException.class, () -> service.updateParameter(
                new UpdateParameterRequestDTO("triage.urgent.sla.hours", "   ", 1L, ADMIN_ID, ADMIN_ROLE)
        ));

        // Saved value unchanged
        assertEquals("4", repository.findByKey("triage.urgent.sla.hours").orElseThrow().getCurrentValue());
        assertTrue(auditService.getRecordedEvents().isEmpty());
    }

    @Test
    @DisplayName("Alt A3 (AC6): Save failure reports error, states previous value remains active, and does not audit")
    void saveFailureReportsPreviousValueActive() {
        ConfigurationParameterRepository mockRepo = mock(ConfigurationParameterRepository.class);
        AccessControlService mockAuth = mock(AccessControlService.class);
        AuditService mockAudit = mock(AuditService.class);
        CatalogueServiceImpl mockService = new CatalogueServiceImpl(mockRepo, mockAuth, mockAudit);

        ConfigurationParameter existing = repository.findByKey("triage.urgent.sla.hours").orElseThrow();
        when(mockAuth.canManageConfiguration(ADMIN_ID, ADMIN_ROLE)).thenReturn(true);
        when(mockRepo.findByKey("triage.urgent.sla.hours")).thenReturn(Optional.of(existing));
        when(mockRepo.save(any())).thenThrow(new IllegalStateException("Disk full"));

        ConfigurationSaveException ex = assertThrows(ConfigurationSaveException.class,
                () -> mockService.updateParameter(new UpdateParameterRequestDTO(
                        "triage.urgent.sla.hours", "6", 1L, ADMIN_ID, ADMIN_ROLE
                )));

        assertEquals("triage.urgent.sla.hours", ex.getParameterKey());
        assertEquals("4", ex.getPreviousValue());
        assertTrue(ex.getMessage().contains("remains active"));
        verify(mockAudit, never()).recordEvent(any());
    }

    @Test
    @DisplayName("Alt A4 (AC4): Concurrent change is detected and rejected without silently overwriting newer value")
    void concurrentChangeRejectedWithConflictDetails() {
        // First admin updates version 1 -> 2
        service.updateParameter(new UpdateParameterRequestDTO(
                "exam.default.duration.minutes", "45", 1L, "admin-1", ADMIN_ROLE
        ));

        // Second admin tries to update using stale version 1
        ConcurrentConfigurationChangeException conflict = assertThrows(
                ConcurrentConfigurationChangeException.class,
                () -> service.updateParameter(new UpdateParameterRequestDTO(
                        "exam.default.duration.minutes", "60", 1L, "admin-2", ADMIN_ROLE
                ))
        );

        assertEquals("exam.default.duration.minutes", conflict.getParameterKey());
        assertEquals(1L, conflict.getExpectedVersion());
        assertEquals(2L, conflict.getActualVersion());
        assertEquals("45", conflict.getCurrentStoredValue());

        // Newer value "45" must NOT be overwritten
        assertEquals("45", repository.findByKey("exam.default.duration.minutes").orElseThrow().getCurrentValue());
    }

    @Test
    @DisplayName("Unknown or blank parameter key throws ConfigurationNotFoundException")
    void unknownParameterThrowsNotFound() {
        assertThrows(ConfigurationNotFoundException.class,
                () -> service.getParameter("non.existent.key", ADMIN_ID, ADMIN_ROLE));
        assertThrows(ConfigurationNotFoundException.class,
                () -> service.getParameter(" ", ADMIN_ID, ADMIN_ROLE));
    }

    @Test
    @DisplayName("Decimal and String parameter validations and PolicyService lookups work correctly")
    void decimalStringValidationAndPolicyService() {
        ConfigurationParameter decimalParam = new ConfigurationParameter(
                "dose.warning.threshold", "Dose Threshold", "SAFETY", "Max dose",
                "1.5", ParameterValueType.DECIMAL, 0.5, 10.0, List.of(),
                ActivationMode.IMMEDIATE, 1L, "system", FIXED_TIME
        );
        assertTrue(decimalParam.validateProposedValue("2.5").valid());
        assertFalse(decimalParam.validateProposedValue("0.1").valid());
        assertFalse(decimalParam.validateProposedValue("12.0").valid());
        assertFalse(decimalParam.validateProposedValue("NaN").valid());
        assertFalse(decimalParam.validateProposedValue("abc").valid());

        ConfigurationParameter stringParam = new ConfigurationParameter(
                "hospital.name", "Hospital Name", "GENERAL", "Display name",
                "SESCAM", ParameterValueType.STRING, null, null, null,
                ActivationMode.IMMEDIATE, 1L, "system", FIXED_TIME
        );
        assertTrue(stringParam.validateProposedValue("Hospital General").valid());
        assertEquals(ActivationMode.IMMEDIATE, stringParam.getActivationMode());

        // PolicyService tests
        PolicyServiceImpl policyService = new PolicyServiceImpl(repository);
        assertEquals(Optional.of("24"), policyService.getPolicyValue("booking.cancellation.min.hours"));
        assertEquals(Optional.empty(), policyService.getPolicyValue(" "));
        assertEquals(24, policyService.getIntPolicy("booking.cancellation.min.hours", 12));
        assertEquals(12, policyService.getIntPolicy("missing.key", 12));
        assertEquals(12, policyService.getIntPolicy("notification.quiet.hours.enabled", 12));
    }

    @Test
    @DisplayName("AuditService receives exact event via Mockito verification")
    void auditServiceReceivesExactEvent() {
        AuditService mockAudit = mock(AuditService.class);
        CatalogueServiceImpl auditedService = new CatalogueServiceImpl(
                repository, accessControlService, mockAudit, Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        auditedService.updateParameter(new UpdateParameterRequestDTO(
                "notification.quiet.hours.enabled", "false", 1L, ADMIN_ID, ADMIN_ROLE
        ));

        ArgumentCaptor<AuditEventDTO> captor = ArgumentCaptor.forClass(AuditEventDTO.class);
        verify(mockAudit, times(1)).recordEvent(captor.capture());
        assertEquals("notification.quiet.hours.enabled", captor.getValue().resourceKey());
        assertEquals("true", captor.getValue().previousValue());
        assertEquals("false", captor.getValue().newValue());
    }
}
