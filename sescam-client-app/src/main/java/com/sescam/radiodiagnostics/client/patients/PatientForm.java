package com.sescam.radiodiagnostics.client.patients;

import com.sescam.radiodiagnostics.common.patients.AddressDTO;
import com.sescam.radiodiagnostics.common.patients.CreatePatientRequestDTO;
import com.sescam.radiodiagnostics.common.patients.DischargePatientRequestDTO;
import com.sescam.radiodiagnostics.common.patients.EmergencyContactDTO;
import com.sescam.radiodiagnostics.common.patients.Gender;
import com.sescam.radiodiagnostics.common.patients.PatientDTO;
import com.sescam.radiodiagnostics.common.patients.PatientStatus;
import com.sescam.radiodiagnostics.common.patients.PreferredNotificationChannel;
import com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class PatientForm {

    private static final Pattern DNI_PATTERN = Pattern.compile("^[0-9]{8}[A-Z]$|^[XYZ][0-9]{7}[A-Z]$", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private String id;
    private String cip;
    private String dni;
    private String firstName;
    private String lastName;
    private String birthDate;
    private Gender gender = Gender.UNKNOWN;
    private String phone;
    private String email;
    private String street;
    private String postalCode;
    private String city;
    private String province;
    private PreferredNotificationChannel preferredChannel = PreferredNotificationChannel.NONE;
    private String emergencyContactName;
    private String emergencyContactRelationship;
    private String emergencyContactPhone;
    private String clinicalSummaryNotes;
    private String dischargeReason;
    private long expectedVersion = 1L;
    private PatientStatus status = PatientStatus.ACTIVE;

    public PatientForm() {
    }

    public static PatientForm fromDTO(PatientDTO dto) {
        PatientForm form = new PatientForm();
        form.setId(dto.id());
        form.setCip(dto.cip());
        form.setDni(dto.dni());
        form.setFirstName(dto.firstName());
        form.setLastName(dto.lastName());
        form.setBirthDate(dto.birthDate() != null ? dto.birthDate().toString() : "");
        form.setGender(dto.gender());
        form.setPhone(dto.phone());
        form.setEmail(dto.email());
        if (dto.address() != null) {
            form.setStreet(dto.address().street());
            form.setPostalCode(dto.address().postalCode());
            form.setCity(dto.address().city());
            form.setProvince(dto.address().province());
        }
        form.setPreferredChannel(dto.preferredChannel());
        if (dto.emergencyContact() != null) {
            form.setEmergencyContactName(dto.emergencyContact().name());
            form.setEmergencyContactRelationship(dto.emergencyContact().relationship());
            form.setEmergencyContactPhone(dto.emergencyContact().phone());
        }
        form.setClinicalSummaryNotes(dto.clinicalSummaryNotes());
        form.setExpectedVersion(dto.version());
        form.setStatus(dto.status());
        return form;
    }

    public Map<String, String> validateRegistration() {
        Map<String, String> errors = new LinkedHashMap<>();
        if (cip == null || cip.trim().length() < 6) {
            errors.put("cip", "CIP is mandatory and must have at least 6 characters.");
        }
        if (dni == null || !DNI_PATTERN.matcher(dni.trim()).matches()) {
            errors.put("dni", "DNI/NIE is mandatory and must follow a valid Spanish format (e.g. 12345678Z).");
        }
        if (firstName == null || firstName.trim().isEmpty()) {
            errors.put("firstName", "First name is mandatory.");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            errors.put("lastName", "Last name is mandatory.");
        }
        if (birthDate == null || birthDate.isBlank()) {
            errors.put("birthDate", "Birth date is mandatory.");
        } else {
            try {
                LocalDate parsed = LocalDate.parse(birthDate.trim());
                if (parsed.isAfter(LocalDate.now())) {
                    errors.put("birthDate", "Birth date cannot be in the future.");
                }
            } catch (DateTimeParseException e) {
                errors.put("birthDate", "Birth date must have format YYYY-MM-DD.");
            }
        }
        if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            errors.put("email", "Email format is invalid.");
        }
        return errors;
    }

    public Map<String, String> validateUpdate() {
        Map<String, String> errors = new LinkedHashMap<>();
        if (firstName != null && firstName.trim().isEmpty()) {
            errors.put("firstName", "First name cannot be empty.");
        }
        if (lastName != null && lastName.trim().isEmpty()) {
            errors.put("lastName", "Last name cannot be empty.");
        }
        if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            errors.put("email", "Email format is invalid.");
        }
        return errors;
    }

    public CreatePatientRequestDTO toCreateRequest(String actorId, String actorRole) {
        LocalDate parsedBirthDate = LocalDate.parse(birthDate.trim());
        AddressDTO address = new AddressDTO(street, postalCode, city, province);
        EmergencyContactDTO emergency = new EmergencyContactDTO(emergencyContactName, emergencyContactRelationship, emergencyContactPhone);
        return new CreatePatientRequestDTO(
                cip, dni, firstName, lastName, parsedBirthDate, gender, phone, email,
                address, preferredChannel, emergency, clinicalSummaryNotes, actorId, actorRole
        );
    }

    public UpdatePatientRequestDTO toUpdateRequest(String actorId, String actorRole) {
        AddressDTO address = new AddressDTO(street, postalCode, city, province);
        EmergencyContactDTO emergency = new EmergencyContactDTO(emergencyContactName, emergencyContactRelationship, emergencyContactPhone);
        return new UpdatePatientRequestDTO(
                id, firstName, lastName, phone, email, address, preferredChannel, emergency,
                clinicalSummaryNotes, expectedVersion, actorId, actorRole
        );
    }

    public DischargePatientRequestDTO toDischargeRequest(String actorId, String actorRole) {
        return new DischargePatientRequestDTO(id, dischargeReason, expectedVersion, actorId, actorRole);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCip() { return cip; }
    public void setCip(String cip) { this.cip = cip; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public PreferredNotificationChannel getPreferredChannel() { return preferredChannel; }
    public void setPreferredChannel(PreferredNotificationChannel preferredChannel) { this.preferredChannel = preferredChannel; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    public String getEmergencyContactRelationship() { return emergencyContactRelationship; }
    public void setEmergencyContactRelationship(String emergencyContactRelationship) { this.emergencyContactRelationship = emergencyContactRelationship; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
    public String getClinicalSummaryNotes() { return clinicalSummaryNotes; }
    public void setClinicalSummaryNotes(String clinicalSummaryNotes) { this.clinicalSummaryNotes = clinicalSummaryNotes; }
    public String getDischargeReason() { return dischargeReason; }
    public void setDischargeReason(String dischargeReason) { this.dischargeReason = dischargeReason; }
    public long getExpectedVersion() { return expectedVersion; }
    public void setExpectedVersion(long expectedVersion) { this.expectedVersion = expectedVersion; }
    public PatientStatus getStatus() { return status; }
    public void setStatus(PatientStatus status) { this.status = status; }
}
