package com.sescam.radiodiagnostics.patients.service;

import com.sescam.radiodiagnostics.common.patients.CreatePatientRequestDTO;
import com.sescam.radiodiagnostics.common.patients.DischargePatientRequestDTO;
import com.sescam.radiodiagnostics.common.patients.DuplicatePatientException;
import com.sescam.radiodiagnostics.common.patients.EhrGateway;
import com.sescam.radiodiagnostics.common.patients.EhrSyncResultDTO;
import com.sescam.radiodiagnostics.common.patients.InvalidPatientDataException;
import com.sescam.radiodiagnostics.common.patients.ConcurrentPatientUpdateException;
import com.sescam.radiodiagnostics.common.patients.PatientDTO;
import com.sescam.radiodiagnostics.common.patients.PatientNotFoundException;
import com.sescam.radiodiagnostics.common.patients.PatientSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.patients.PatientService;
import com.sescam.radiodiagnostics.common.patients.PatientStatus;
import com.sescam.radiodiagnostics.common.patients.PatientSummaryDTO;
import com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO;
import com.sescam.radiodiagnostics.common.patients.UnauthorizedPatientAccessException;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.patients.domain.Address;
import com.sescam.radiodiagnostics.patients.domain.EmergencyContact;
import com.sescam.radiodiagnostics.patients.domain.Patient;
import com.sescam.radiodiagnostics.patients.repository.PatientRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Realization of UC-AAD-01: Manage Patient.
 */
public class PatientServiceImpl implements PatientService {

    private static final Pattern DNI_PATTERN = Pattern.compile("^[0-9]{8}[A-Z]$|^[XYZ][0-9]{7}[A-Z]$", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private final PatientRepository patientRepository;
    private final AccessControlService accessControlService;
    private final AuditService auditService;
    private final EhrGateway ehrGateway;
    private final Clock clock;

    public PatientServiceImpl(PatientRepository patientRepository,
                              AccessControlService accessControlService,
                              AuditService auditService,
                              EhrGateway ehrGateway) {
        this(patientRepository, accessControlService, auditService, ehrGateway, Clock.systemUTC());
    }

    public PatientServiceImpl(PatientRepository patientRepository,
                              AccessControlService accessControlService,
                              AuditService auditService,
                              EhrGateway ehrGateway,
                              Clock clock) {
        this.patientRepository = Objects.requireNonNull(patientRepository, "patientRepository must not be null");
        this.accessControlService = Objects.requireNonNull(accessControlService, "accessControlService must not be null");
        this.auditService = Objects.requireNonNull(auditService, "auditService must not be null");
        this.ehrGateway = Objects.requireNonNull(ehrGateway, "ehrGateway must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public PatientDTO registerPatient(CreatePatientRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");
        checkAuthorization(request.actorId(), request.actorRole(), "REGISTER_PATIENT");

        validateRegistration(request);

        String cip = request.cip().trim().toUpperCase(Locale.ROOT);
        String dni = request.dni().trim().toUpperCase(Locale.ROOT);

        if (patientRepository.existsByCip(cip)) {
            audit(request.actorId(), "PATIENT_REGISTRATION_REJECTED", cip, "Duplicate CIP: " + cip);
            throw new DuplicatePatientException("Patient with CIP '" + cip + "' is already registered.");
        }
        if (patientRepository.existsByDni(dni)) {
            audit(request.actorId(), "PATIENT_REGISTRATION_REJECTED", dni, "Duplicate DNI: " + dni);
            throw new DuplicatePatientException("Patient with DNI '" + dni + "' is already registered.");
        }

        Instant now = clock.instant();
        String id = "PAT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);

        Address address = request.address() == null
                ? new Address("", "", "", "")
                : new Address(request.address().street(), request.address().postalCode(), request.address().city(), request.address().province());

        EmergencyContact contact = request.emergencyContact() == null
                ? new EmergencyContact("", "", "")
                : new EmergencyContact(request.emergencyContact().name(), request.emergencyContact().relationship(), request.emergencyContact().phone());

        Patient patient = new Patient(
                id, cip, dni, request.firstName().trim(), request.lastName().trim(),
                request.birthDate(), request.gender(), request.phone(), request.email(),
                address, request.preferredChannel(), contact, request.clinicalSummaryNotes(),
                PatientStatus.ACTIVE, 1L, now, now, "PENDING_SYNC", null
        );

        Patient saved = patientRepository.save(patient);
        audit(request.actorId(), "PATIENT_REGISTERED", saved.getId(), "Registered patient: " + saved.getCip());

        // External EHR synchronization hook (A3)
        PatientDTO dto = saved.toDTO();
        EhrSyncResultDTO syncResult = ehrGateway.syncPatientRecord(dto);
        if (syncResult != null && syncResult.synced()) {
            saved.markEhrSync("SYNCED", syncResult.syncTimestamp());
        } else {
            String reason = syncResult != null ? syncResult.message() : "Unknown synchronization error";
            saved.markEhrSync("FAILED_SYNC", clock.instant());
            audit(request.actorId(), "PATIENT_EHR_SYNC_FAILED", saved.getId(), reason);
        }

        return saved.toDTO();
    }

    @Override
    public PatientDTO getPatientById(String patientId, String actorId, String actorRole) {
        checkAuthorization(actorId, actorRole, "GET_PATIENT");
        if (patientId == null || patientId.isBlank()) {
            throw new InvalidPatientDataException("Patient ID must not be blank.");
        }
        return patientRepository.findById(patientId.trim())
                .map(Patient::toDTO)
                .orElseThrow(() -> new PatientNotFoundException("Patient with ID '" + patientId + "' not found."));
    }

    @Override
    public PatientDTO getPatientByCip(String cip, String actorId, String actorRole) {
        checkAuthorization(actorId, actorRole, "GET_PATIENT");
        if (cip == null || cip.isBlank()) {
            throw new InvalidPatientDataException("CIP must not be blank.");
        }
        return patientRepository.findByCip(cip.trim())
                .map(Patient::toDTO)
                .orElseThrow(() -> new PatientNotFoundException("Patient with CIP '" + cip + "' not found."));
    }

    @Override
    public List<PatientSummaryDTO> searchPatients(PatientSearchCriteriaDTO criteria, String actorId, String actorRole) {
        checkAuthorization(actorId, actorRole, "SEARCH_PATIENTS");
        PatientSearchCriteriaDTO effective = criteria == null ? PatientSearchCriteriaDTO.ofQuery("") : criteria;
        return patientRepository.search(effective).stream()
                .map(Patient::toSummaryDTO)
                .toList();
    }

    @Override
    public PatientDTO updatePatient(UpdatePatientRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");
        checkAuthorization(request.actorId(), request.actorRole(), "UPDATE_PATIENT");

        if (request.patientId() == null || request.patientId().isBlank()) {
            throw new InvalidPatientDataException("Patient ID must not be blank.");
        }

        Patient patient = patientRepository.findById(request.patientId().trim())
                .orElseThrow(() -> new PatientNotFoundException("Patient with ID '" + request.patientId() + "' not found."));

        // A4 Concurrency check
        if (patient.getVersion() != request.expectedVersion()) {
            throw new ConcurrentPatientUpdateException("Patient record has been modified concurrently. Expected version: "
                    + request.expectedVersion() + ", actual version: " + patient.getVersion());
        }

        validateUpdate(request);

        Address address = request.address() == null ? null
                : new Address(request.address().street(), request.address().postalCode(), request.address().city(), request.address().province());

        EmergencyContact contact = request.emergencyContact() == null ? null
                : new EmergencyContact(request.emergencyContact().name(), request.emergencyContact().relationship(), request.emergencyContact().phone());

        Instant now = clock.instant();
        patient.updateDetails(
                request.firstName(), request.lastName(), request.phone(), request.email(),
                address, request.preferredChannel(), contact, request.clinicalSummaryNotes(),
                now
        );

        Patient saved = patientRepository.save(patient);
        audit(request.actorId(), "PATIENT_UPDATED", saved.getId(), "Updated patient version " + saved.getVersion());

        // EHR sync
        EhrSyncResultDTO syncResult = ehrGateway.syncPatientRecord(saved.toDTO());
        if (syncResult != null && syncResult.synced()) {
            saved.markEhrSync("SYNCED", syncResult.syncTimestamp());
        } else {
            saved.markEhrSync("FAILED_SYNC", now);
        }

        return saved.toDTO();
    }

    @Override
    public PatientDTO dischargePatient(DischargePatientRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");
        checkAuthorization(request.actorId(), request.actorRole(), "DISCHARGE_PATIENT");

        if (request.patientId() == null || request.patientId().isBlank()) {
            throw new InvalidPatientDataException("Patient ID must not be blank.");
        }

        Patient patient = patientRepository.findById(request.patientId().trim())
                .orElseThrow(() -> new PatientNotFoundException("Patient with ID '" + request.patientId() + "' not found."));

        // Concurrency check
        if (patient.getVersion() != request.expectedVersion()) {
            throw new ConcurrentPatientUpdateException("Patient record has been modified concurrently. Expected version: "
                    + request.expectedVersion() + ", actual version: " + patient.getVersion());
        }

        Instant now = clock.instant();
        patient.discharge(request.dischargeReason(), now);
        Patient saved = patientRepository.save(patient);
        audit(request.actorId(), "PATIENT_DISCHARGED", saved.getId(), "Discharged patient. Reason: " + request.dischargeReason());

        EhrSyncResultDTO syncResult = ehrGateway.syncPatientRecord(saved.toDTO());
        if (syncResult != null && syncResult.synced()) {
            saved.markEhrSync("SYNCED", syncResult.syncTimestamp());
        } else {
            saved.markEhrSync("FAILED_SYNC", now);
        }

        return saved.toDTO();
    }

    private void checkAuthorization(String actorId, String actorRole, String operation) {
        if (!accessControlService.canManagePatients(actorId, actorRole)) {
            audit(actorId, "PATIENT_ACCESS_DENIED", operation, "User " + actorId + " with role " + actorRole + " attempted " + operation);
            throw new UnauthorizedPatientAccessException("User '" + actorId + "' with role '" + actorRole + "' is not authorized to manage patients.");
        }
    }

    private void validateRegistration(CreatePatientRequestDTO req) {
        if (req.cip() == null || req.cip().trim().length() < 6) {
            throw new InvalidPatientDataException("CIP is mandatory and must contain at least 6 characters.");
        }
        if (req.dni() == null || !DNI_PATTERN.matcher(req.dni().trim()).matches()) {
            throw new InvalidPatientDataException("DNI/NIE is mandatory and must have a valid Spanish format (e.g., 12345678Z).");
        }
        if (req.firstName() == null || req.firstName().trim().isEmpty()) {
            throw new InvalidPatientDataException("First name is mandatory.");
        }
        if (req.lastName() == null || req.lastName().trim().isEmpty()) {
            throw new InvalidPatientDataException("Last name is mandatory.");
        }
        if (req.birthDate() == null) {
            throw new InvalidPatientDataException("Birth date is mandatory.");
        }
        LocalDate today = LocalDate.now(clock);
        if (req.birthDate().isAfter(today)) {
            throw new InvalidPatientDataException("Birth date cannot be in the future.");
        }
        if (req.email() != null && !req.email().isBlank() && !EMAIL_PATTERN.matcher(req.email().trim()).matches()) {
            throw new InvalidPatientDataException("Email format is invalid.");
        }
    }

    private void validateUpdate(UpdatePatientRequestDTO req) {
        if (req.firstName() != null && req.firstName().trim().isEmpty()) {
            throw new InvalidPatientDataException("First name cannot be empty.");
        }
        if (req.lastName() != null && req.lastName().trim().isEmpty()) {
            throw new InvalidPatientDataException("Last name cannot be empty.");
        }
        if (req.email() != null && !req.email().isBlank() && !EMAIL_PATTERN.matcher(req.email().trim()).matches()) {
            throw new InvalidPatientDataException("Email format is invalid.");
        }
    }

    private void audit(String actorId, String action, String resourceKey, String details) {
        try {
            auditService.recordEvent(new AuditEventDTO(
                    action,
                    actorId == null ? "SYSTEM" : actorId,
                    resourceKey,
                    null,
                    null,
                    details,
                    clock.instant()
            ));
        } catch (Exception ignored) {
            // Audit failures must not abort the business transaction unless strict
        }
    }
}
