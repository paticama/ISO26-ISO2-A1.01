package com.sescam.radiodiagnostics.patients.service;

import com.sescam.radiodiagnostics.common.patients.AddressDTO;
import com.sescam.radiodiagnostics.common.patients.CreatePatientRequestDTO;
import com.sescam.radiodiagnostics.common.patients.DischargePatientRequestDTO;
import com.sescam.radiodiagnostics.common.patients.DuplicatePatientException;
import com.sescam.radiodiagnostics.common.patients.EmergencyContactDTO;
import com.sescam.radiodiagnostics.common.patients.Gender;
import com.sescam.radiodiagnostics.common.patients.InvalidPatientDataException;
import com.sescam.radiodiagnostics.common.patients.ConcurrentPatientUpdateException;
import com.sescam.radiodiagnostics.common.patients.PatientDTO;
import com.sescam.radiodiagnostics.common.patients.PatientNotFoundException;
import com.sescam.radiodiagnostics.common.patients.PatientSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.patients.PatientStatus;
import com.sescam.radiodiagnostics.common.patients.PatientSummaryDTO;
import com.sescam.radiodiagnostics.common.patients.PreferredNotificationChannel;
import com.sescam.radiodiagnostics.common.patients.UnauthorizedPatientAccessException;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.patients.ehr.NoOpEhrGateway;
import com.sescam.radiodiagnostics.patients.repository.InMemoryPatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatientServiceImplTest {

    private InMemoryPatientRepository repository;
    private AccessControlService accessControlService;
    private RecordingAuditService auditService;
    private NoOpEhrGateway ehrGateway;
    private PatientServiceImpl service;
    private Clock fixedClock;

    private static final String ADMIN_STAFF_ID = "adm-user-1";
    private static final String ADMIN_STAFF_ROLE = "ADMINISTRATIVE_STAFF";

    @BeforeEach
    void setUp() {
        repository = new InMemoryPatientRepository();
        accessControlService = (actorId, role) -> role != null && (role.contains("ADMIN") || role.contains("ADMISSION"));
        auditService = new RecordingAuditService();
        ehrGateway = new NoOpEhrGateway();
        fixedClock = Clock.fixed(Instant.parse("2026-11-23T10:00:00Z"), ZoneId.of("UTC"));

        service = new PatientServiceImpl(repository, accessControlService, auditService, ehrGateway, fixedClock);
    }

    @Test
    @DisplayName("Main Success Scenario: Registers patient, audits event, and synchronizes EHR")
    void registerPatientSuccess() {
        CreatePatientRequestDTO req = sampleCreateRequest("CIP12345678", "12345678Z");

        PatientDTO patient = service.registerPatient(req);

        assertThat(patient.id()).isNotNull().startsWith("PAT-");
        assertThat(patient.cip()).isEqualTo("CIP12345678");
        assertThat(patient.dni()).isEqualTo("12345678Z");
        assertThat(patient.firstName()).isEqualTo("Carlos");
        assertThat(patient.lastName()).isEqualTo("Sanchez");
        assertThat(patient.status()).isEqualTo(PatientStatus.ACTIVE);
        assertThat(patient.version()).isEqualTo(1L);
        assertThat(patient.ehrSyncStatus()).isEqualTo("SYNCED");

        // Audit check
        assertThat(auditService.events).anyMatch(e -> e.action().equals("PATIENT_REGISTERED")
                && e.actorId().equals(ADMIN_STAFF_ID)
                && e.resourceKey().equals(patient.id()));
    }

    @Test
    @DisplayName("A1: Possible duplicate record (existing CIP or DNI) is rejected")
    void duplicateRecordRejected() {
        service.registerPatient(sampleCreateRequest("CIP11112222", "11112222A"));

        // Duplicate CIP
        CreatePatientRequestDTO dupCip = sampleCreateRequest("CIP11112222", "99998888X");
        assertThatThrownBy(() -> service.registerPatient(dupCip))
                .isInstanceOf(DuplicatePatientException.class)
                .hasMessageContaining("CIP");

        // Duplicate DNI
        CreatePatientRequestDTO dupDni = sampleCreateRequest("CIP99998888", "11112222A");
        assertThatThrownBy(() -> service.registerPatient(dupDni))
                .isInstanceOf(DuplicatePatientException.class)
                .hasMessageContaining("DNI");

        assertThat(auditService.events).anyMatch(e -> e.action().equals("PATIENT_REGISTRATION_REJECTED"));
    }

    @Test
    @DisplayName("A2: Invalid or incomplete data fails validation")
    void validationFailures() {
        // Missing / short CIP
        CreatePatientRequestDTO shortCip = new CreatePatientRequestDTO(
                "CIP", "12345678Z", "A", "B", LocalDate.of(1990, 1, 1),
                Gender.MALE, "600123456", "a@b.com", null, null, null, null,
                ADMIN_STAFF_ID, ADMIN_STAFF_ROLE
        );
        assertThatThrownBy(() -> service.registerPatient(shortCip))
                .isInstanceOf(InvalidPatientDataException.class)
                .hasMessageContaining("CIP");

        // Invalid DNI
        CreatePatientRequestDTO badDni = new CreatePatientRequestDTO(
                "CIP12345678", "INVALID", "A", "B", LocalDate.of(1990, 1, 1),
                Gender.MALE, "600123456", "a@b.com", null, null, null, null,
                ADMIN_STAFF_ID, ADMIN_STAFF_ROLE
        );
        assertThatThrownBy(() -> service.registerPatient(badDni))
                .isInstanceOf(InvalidPatientDataException.class)
                .hasMessageContaining("DNI");

        // Future birth date
        CreatePatientRequestDTO futureBirth = new CreatePatientRequestDTO(
                "CIP12345678", "12345678Z", "A", "B", LocalDate.of(2030, 1, 1),
                Gender.MALE, "600123456", "a@b.com", null, null, null, null,
                ADMIN_STAFF_ID, ADMIN_STAFF_ROLE
        );
        assertThatThrownBy(() -> service.registerPatient(futureBirth))
                .isInstanceOf(InvalidPatientDataException.class)
                .hasMessageContaining("future");

        // Bad email format
        CreatePatientRequestDTO badEmail = new CreatePatientRequestDTO(
                "CIP12345678", "12345678Z", "A", "B", LocalDate.of(1990, 1, 1),
                Gender.MALE, "600123456", "bad-email", null, null, null, null,
                ADMIN_STAFF_ID, ADMIN_STAFF_ROLE
        );
        assertThatThrownBy(() -> service.registerPatient(badEmail))
                .isInstanceOf(InvalidPatientDataException.class)
                .hasMessageContaining("Email");
    }

    @Test
    @DisplayName("A3: EHR synchronization failure marks status as FAILED_SYNC and logs audit event")
    void ehrSyncFailureHandled() {
        ehrGateway.setSimulateFailure(true);

        PatientDTO patient = service.registerPatient(sampleCreateRequest("CIP77778888", "77778888K"));

        assertThat(patient.ehrSyncStatus()).isEqualTo("FAILED_SYNC");
        assertThat(auditService.events).anyMatch(e -> e.action().equals("PATIENT_EHR_SYNC_FAILED"));
    }

    @Test
    @DisplayName("A4: Concurrent update conflict throws ConcurrentPatientUpdateException")
    void concurrentUpdateConflict() {
        PatientDTO created = service.registerPatient(sampleCreateRequest("CIP55556666", "55556666M"));

        // First update works
        com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO update1 =
                new com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO(
                        created.id(), "Carlos Updated", "Sanchez", "611000111", "carlos@test.es",
                        null, null, null, null, 1L, ADMIN_STAFF_ID, ADMIN_STAFF_ROLE
                );
        PatientDTO updated = service.updatePatient(update1);
        assertThat(updated.version()).isEqualTo(2L);

        // Stale update with version 1L fails
        com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO staleUpdate =
                new com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO(
                        created.id(), "Stale update", "Sanchez", "611000111", "carlos@test.es",
                        null, null, null, null, 1L, ADMIN_STAFF_ID, ADMIN_STAFF_ROLE
                );
        assertThatThrownBy(() -> service.updatePatient(staleUpdate))
                .isInstanceOf(ConcurrentPatientUpdateException.class);
    }

    @Test
    @DisplayName("A5: Unauthorized operation is blocked and logged")
    void unauthorizedAccessBlocked() {
        CreatePatientRequestDTO req = new CreatePatientRequestDTO(
                "CIP99990000", "99990000P", "Unauthorized", "User", LocalDate.of(1995, 1, 1),
                Gender.OTHER, "600111222", "unauth@test.es", null, null, null, null,
                "attacker", "UNAUTHORIZED_ROLE"
        );

        assertThatThrownBy(() -> service.registerPatient(req))
                .isInstanceOf(UnauthorizedPatientAccessException.class);

        assertThat(auditService.events).anyMatch(e -> e.action().equals("PATIENT_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Discharge updates patient status and records audit log")
    void dischargePatient() {
        PatientDTO patient = service.registerPatient(sampleCreateRequest("CIP44445555", "44445555L"));

        DischargePatientRequestDTO dischargeReq = new DischargePatientRequestDTO(
                patient.id(), "Patient moved out of province", 1L, ADMIN_STAFF_ID, ADMIN_STAFF_ROLE
        );

        PatientDTO discharged = service.dischargePatient(dischargeReq);
        assertThat(discharged.status()).isEqualTo(PatientStatus.DISCHARGED);
        assertThat(discharged.version()).isEqualTo(2L);

        assertThat(auditService.events).anyMatch(e -> e.action().equals("PATIENT_DISCHARGED"));
    }

    @Test
    @DisplayName("Search and consult by ID and CIP retrieve correct records")
    void searchAndConsult() {
        PatientDTO p = service.registerPatient(sampleCreateRequest("CIP33334444", "33334444J"));

        PatientDTO byId = service.getPatientById(p.id(), ADMIN_STAFF_ID, ADMIN_STAFF_ROLE);
        assertThat(byId.cip()).isEqualTo("CIP33334444");

        PatientDTO byCip = service.getPatientByCip("CIP33334444", ADMIN_STAFF_ID, ADMIN_STAFF_ROLE);
        assertThat(byCip.id()).isEqualTo(p.id());

        List<PatientSummaryDTO> results = service.searchPatients(
                PatientSearchCriteriaDTO.ofQuery("Sanchez"), ADMIN_STAFF_ID, ADMIN_STAFF_ROLE
        );
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("Edge cases: Consult, update, and discharge with invalid, blank, or non-existent IDs")
    void invalidAndNonExistentIds() {
        assertThatThrownBy(() -> service.getPatientById(" ", ADMIN_STAFF_ID, ADMIN_STAFF_ROLE))
                .isInstanceOf(InvalidPatientDataException.class);
        assertThatThrownBy(() -> service.getPatientById("PAT-99999", ADMIN_STAFF_ID, ADMIN_STAFF_ROLE))
                .isInstanceOf(PatientNotFoundException.class);

        assertThatThrownBy(() -> service.getPatientByCip(" ", ADMIN_STAFF_ID, ADMIN_STAFF_ROLE))
                .isInstanceOf(InvalidPatientDataException.class);
        assertThatThrownBy(() -> service.getPatientByCip("CIP-NONE", ADMIN_STAFF_ID, ADMIN_STAFF_ROLE))
                .isInstanceOf(PatientNotFoundException.class);

        com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO blankIdUpdate =
                new com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO(
                        " ", "Carlos", "Sanchez", "611000111", "carlos@test.es",
                        null, null, null, null, 1L, ADMIN_STAFF_ID, ADMIN_STAFF_ROLE
                );
        assertThatThrownBy(() -> service.updatePatient(blankIdUpdate))
                .isInstanceOf(InvalidPatientDataException.class);

        com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO nonExistentUpdate =
                new com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO(
                        "PAT-99999", "Carlos", "Sanchez", "611000111", "carlos@test.es",
                        null, null, null, null, 1L, ADMIN_STAFF_ID, ADMIN_STAFF_ROLE
                );
        assertThatThrownBy(() -> service.updatePatient(nonExistentUpdate))
                .isInstanceOf(PatientNotFoundException.class);

        DischargePatientRequestDTO blankDischarge = new DischargePatientRequestDTO(" ", "reason", 1L, ADMIN_STAFF_ID, ADMIN_STAFF_ROLE);
        assertThatThrownBy(() -> service.dischargePatient(blankDischarge))
                .isInstanceOf(InvalidPatientDataException.class);

        DischargePatientRequestDTO nonExistentDischarge = new DischargePatientRequestDTO("PAT-99999", "reason", 1L, ADMIN_STAFF_ID, ADMIN_STAFF_ROLE);
        assertThatThrownBy(() -> service.dischargePatient(nonExistentDischarge))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    @DisplayName("Edge cases: Update validations and discharge version mismatch")
    void updateValidationAndDischargeConflict() {
        PatientDTO created = service.registerPatient(sampleCreateRequest("CIP66667777", "66667777N"));

        com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO emptyFirstName =
                new com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO(
                        created.id(), " ", "Sanchez", "611000111", "carlos@test.es",
                        null, null, null, null, 1L, ADMIN_STAFF_ID, ADMIN_STAFF_ROLE
                );
        assertThatThrownBy(() -> service.updatePatient(emptyFirstName))
                .isInstanceOf(InvalidPatientDataException.class);

        com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO badEmail =
                new com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO(
                        created.id(), "Carlos", "Sanchez", "611000111", "bad-email",
                        null, null, null, null, 1L, ADMIN_STAFF_ID, ADMIN_STAFF_ROLE
                );
        assertThatThrownBy(() -> service.updatePatient(badEmail))
                .isInstanceOf(InvalidPatientDataException.class);

        DischargePatientRequestDTO conflictDischarge = new DischargePatientRequestDTO(
                created.id(), "reason", 99L, ADMIN_STAFF_ID, ADMIN_STAFF_ROLE
        );
        assertThatThrownBy(() -> service.dischargePatient(conflictDischarge))
                .isInstanceOf(ConcurrentPatientUpdateException.class);
    }

    private CreatePatientRequestDTO sampleCreateRequest(String cip, String dni) {
        AddressDTO address = new AddressDTO("Av. de los Descubrimientos 4", "13071", "Ciudad Real", "Ciudad Real");
        EmergencyContactDTO contact = new EmergencyContactDTO("Laura Sanchez", "Sister", "655444333");
        return new CreatePatientRequestDTO(
                cip, dni, "Carlos", "Sanchez", LocalDate.of(1988, 4, 12),
                Gender.MALE, "655111222", "carlos.sanchez@sescam.jccm.es",
                address, PreferredNotificationChannel.SMS, contact, "No known drug allergies",
                ADMIN_STAFF_ID, ADMIN_STAFF_ROLE
        );
    }

    private static class RecordingAuditService implements AuditService {
        final List<AuditEventDTO> events = new ArrayList<>();

        @Override
        public void recordEvent(AuditEventDTO event) {
            events.add(event);
        }
    }
}
