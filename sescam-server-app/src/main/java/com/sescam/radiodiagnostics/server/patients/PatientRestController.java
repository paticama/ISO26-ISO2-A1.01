package com.sescam.radiodiagnostics.server.patients;

import com.sescam.radiodiagnostics.common.patients.AddressDTO;
import com.sescam.radiodiagnostics.common.patients.CreatePatientRequestDTO;
import com.sescam.radiodiagnostics.common.patients.DischargePatientRequestDTO;
import com.sescam.radiodiagnostics.common.patients.EmergencyContactDTO;
import com.sescam.radiodiagnostics.common.patients.Gender;
import com.sescam.radiodiagnostics.common.patients.PatientDTO;
import com.sescam.radiodiagnostics.common.patients.PatientSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.patients.PatientService;
import com.sescam.radiodiagnostics.common.patients.PatientStatus;
import com.sescam.radiodiagnostics.common.patients.PatientSummaryDTO;
import com.sescam.radiodiagnostics.common.patients.PreferredNotificationChannel;
import com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST API for UC-AAD-01: Manage Patient.
 */
@RestController
@RequestMapping("/api/v1/patients")
public class PatientRestController {

    private final PatientService patientService;

    public PatientRestController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    public ResponseEntity<PatientDTO> register(
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestBody CreatePatientPayload payload) {
        CreatePatientRequestDTO request = new CreatePatientRequestDTO(
                payload.cip(), payload.dni(), payload.firstName(), payload.lastName(),
                payload.birthDate(), payload.gender(), payload.phone(), payload.email(),
                payload.address(), payload.preferredChannel(), payload.emergencyContact(),
                payload.clinicalSummaryNotes(), actorId, actorRole
        );
        PatientDTO created = patientService.registerPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<PatientSummaryDTO>> search(
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestParam(value = "query", defaultValue = "") String query,
            @RequestParam(value = "status", required = false) PatientStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        PatientSearchCriteriaDTO criteria = new PatientSearchCriteriaDTO(query, status, page, size);
        return ResponseEntity.ok(patientService.searchPatients(criteria, actorId, actorRole));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientDTO> getById(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.ok(patientService.getPatientById(id, actorId, actorRole));
    }

    @GetMapping("/cip/{cip}")
    public ResponseEntity<PatientDTO> getByCip(
            @PathVariable("cip") String cip,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        return ResponseEntity.ok(patientService.getPatientByCip(cip, actorId, actorRole));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientDTO> update(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestBody UpdatePatientPayload payload) {
        UpdatePatientRequestDTO request = new UpdatePatientRequestDTO(
                id, payload.firstName(), payload.lastName(), payload.phone(), payload.email(),
                payload.address(), payload.preferredChannel(), payload.emergencyContact(),
                payload.clinicalSummaryNotes(), payload.expectedVersion(), actorId, actorRole
        );
        return ResponseEntity.ok(patientService.updatePatient(request));
    }

    @PostMapping("/{id}/discharge")
    public ResponseEntity<PatientDTO> discharge(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestBody DischargePayload payload) {
        DischargePatientRequestDTO request = new DischargePatientRequestDTO(
                id, payload.dischargeReason(), payload.expectedVersion(), actorId, actorRole
        );
        return ResponseEntity.ok(patientService.dischargePatient(request));
    }

    public record CreatePatientPayload(
            String cip,
            String dni,
            String firstName,
            String lastName,
            LocalDate birthDate,
            Gender gender,
            String phone,
            String email,
            AddressDTO address,
            PreferredNotificationChannel preferredChannel,
            EmergencyContactDTO emergencyContact,
            String clinicalSummaryNotes
    ) {}

    public record UpdatePatientPayload(
            String firstName,
            String lastName,
            String phone,
            String email,
            AddressDTO address,
            PreferredNotificationChannel preferredChannel,
            EmergencyContactDTO emergencyContact,
            String clinicalSummaryNotes,
            long expectedVersion
    ) {}

    public record DischargePayload(
            String dischargeReason,
            long expectedVersion
    ) {}
}
