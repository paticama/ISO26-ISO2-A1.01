package com.sescam.radiodiagnostics.patients.domain;

import com.sescam.radiodiagnostics.common.patients.AddressDTO;
import com.sescam.radiodiagnostics.common.patients.EmergencyContactDTO;
import com.sescam.radiodiagnostics.common.patients.Gender;
import com.sescam.radiodiagnostics.common.patients.PatientDTO;
import com.sescam.radiodiagnostics.common.patients.PatientStatus;
import com.sescam.radiodiagnostics.common.patients.PatientSummaryDTO;
import com.sescam.radiodiagnostics.common.patients.PreferredNotificationChannel;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Domain entity representing an individual registered in the SESCAM Master Patient Index.
 */
public class Patient {

    private final String id;
    private final String cip;
    private final String dni;
    private String firstName;
    private String lastName;
    private final LocalDate birthDate;
    private final Gender gender;
    private String phone;
    private String email;
    private Address address;
    private PreferredNotificationChannel preferredChannel;
    private EmergencyContact emergencyContact;
    private String clinicalSummaryNotes;
    private PatientStatus status;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;
    private String ehrSyncStatus;
    private Instant ehrSyncTimestamp;
    private String dischargeReason;

    public Patient(String id, String cip, String dni, String firstName, String lastName,
                   LocalDate birthDate, Gender gender, String phone, String email,
                   Address address, PreferredNotificationChannel preferredChannel,
                   EmergencyContact emergencyContact, String clinicalSummaryNotes,
                   PatientStatus status, long version, Instant createdAt, Instant updatedAt,
                   String ehrSyncStatus, Instant ehrSyncTimestamp) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.cip = Objects.requireNonNull(cip, "cip must not be null");
        this.dni = Objects.requireNonNull(dni, "dni must not be null");
        this.firstName = Objects.requireNonNull(firstName, "firstName must not be null");
        this.lastName = Objects.requireNonNull(lastName, "lastName must not be null");
        this.birthDate = Objects.requireNonNull(birthDate, "birthDate must not be null");
        this.gender = gender == null ? Gender.UNKNOWN : gender;
        this.phone = phone == null ? "" : phone.trim();
        this.email = email == null ? "" : email.trim();
        this.address = address == null ? new Address("", "", "", "") : address;
        this.preferredChannel = preferredChannel == null ? PreferredNotificationChannel.NONE : preferredChannel;
        this.emergencyContact = emergencyContact == null ? new EmergencyContact("", "", "") : emergencyContact;
        this.clinicalSummaryNotes = clinicalSummaryNotes == null ? "" : clinicalSummaryNotes.trim();
        this.status = status == null ? PatientStatus.ACTIVE : status;
        this.version = version;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
        this.ehrSyncStatus = ehrSyncStatus == null ? "NOT_SYNCED" : ehrSyncStatus;
        this.ehrSyncTimestamp = ehrSyncTimestamp;
    }

    public void updateDetails(String firstName, String lastName, String phone, String email,
                              Address address, PreferredNotificationChannel preferredChannel,
                              EmergencyContact emergencyContact, String clinicalSummaryNotes,
                              Instant now) {
        if (firstName != null && !firstName.isBlank()) this.firstName = firstName.trim();
        if (lastName != null && !lastName.isBlank()) this.lastName = lastName.trim();
        if (phone != null) this.phone = phone.trim();
        if (email != null) this.email = email.trim();
        if (address != null) this.address = address;
        if (preferredChannel != null) this.preferredChannel = preferredChannel;
        if (emergencyContact != null) this.emergencyContact = emergencyContact;
        if (clinicalSummaryNotes != null) this.clinicalSummaryNotes = clinicalSummaryNotes.trim();

        this.version++;
        this.updatedAt = now == null ? Instant.now() : now;
    }

    public void discharge(String reason, Instant now) {
        this.status = PatientStatus.DISCHARGED;
        this.dischargeReason = reason == null ? "" : reason.trim();
        this.version++;
        this.updatedAt = now == null ? Instant.now() : now;
    }

    public void markEhrSync(String syncStatus, Instant timestamp) {
        this.ehrSyncStatus = syncStatus;
        this.ehrSyncTimestamp = timestamp;
    }

    public PatientDTO toDTO() {
        AddressDTO addrDTO = new AddressDTO(address.street(), address.postalCode(), address.city(), address.province());
        EmergencyContactDTO ecDTO = new EmergencyContactDTO(emergencyContact.name(), emergencyContact.relationship(), emergencyContact.phone());
        return new PatientDTO(
                id, cip, dni, firstName, lastName, birthDate, gender, phone, email,
                addrDTO, preferredChannel, ecDTO, clinicalSummaryNotes,
                status, version, createdAt, updatedAt, ehrSyncStatus, ehrSyncTimestamp
        );
    }

    public PatientSummaryDTO toSummaryDTO() {
        String fullName = firstName + " " + lastName;
        return new PatientSummaryDTO(id, cip, dni, fullName, birthDate, phone, status, version);
    }

    public String getId() { return id; }
    public String getCip() { return cip; }
    public String getDni() { return dni; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public LocalDate getBirthDate() { return birthDate; }
    public Gender getGender() { return gender; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public Address getAddress() { return address; }
    public PreferredNotificationChannel getPreferredChannel() { return preferredChannel; }
    public EmergencyContact getEmergencyContact() { return emergencyContact; }
    public String getClinicalSummaryNotes() { return clinicalSummaryNotes; }
    public PatientStatus getStatus() { return status; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getEhrSyncStatus() { return ehrSyncStatus; }
    public Instant getEhrSyncTimestamp() { return ehrSyncTimestamp; }
    public String getDischargeReason() { return dischargeReason; }
}
