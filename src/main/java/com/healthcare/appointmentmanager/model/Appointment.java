package com.healthcare.appointmentmanager.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "appointments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_active_reservation_key", columnNames = "reservation_key")
})
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private DoctorProfile doctor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private AppUser patient;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(nullable = false)
    private String symptoms;

    @JdbcTypeCode(Types.LONGVARCHAR)
    private String preVisitSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UrgencyLevel urgencyLevel = UrgencyLevel.NOT_ASSESSED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.HELD;

    @Column(name = "reservation_key", unique = true)
    private String reservationKey;

    private LocalDateTime holdExpiresAt;

    @JdbcTypeCode(Types.LONGVARCHAR)
    private String clinicalNotes;

    @JdbcTypeCode(Types.LONGVARCHAR)
    private String prescription;

    @JdbcTypeCode(Types.LONGVARCHAR)
    private String postVisitSummary;

    @JdbcTypeCode(Types.LONGVARCHAR)
    private String followUpInstructions;

    @Enumerated(EnumType.STRING)
    private MedicationFrequency medicationFrequency = MedicationFrequency.NONE;

    private LocalDate medicationStartDate;
    private LocalDate medicationEndDate;
    private String googleEventId;
    private Long calendarOwnerUserId;
    private boolean appointmentReminderQueued;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Appointment() {
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public DoctorProfile getDoctor() { return doctor; }
    public void setDoctor(DoctorProfile doctor) { this.doctor = doctor; }
    public AppUser getPatient() { return patient; }
    public void setPatient(AppUser patient) { this.patient = patient; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
    public String getPreVisitSummary() { return preVisitSummary; }
    public void setPreVisitSummary(String preVisitSummary) { this.preVisitSummary = preVisitSummary; }
    public UrgencyLevel getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(UrgencyLevel urgencyLevel) { this.urgencyLevel = urgencyLevel; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public String getReservationKey() { return reservationKey; }
    public void setReservationKey(String reservationKey) { this.reservationKey = reservationKey; }
    public LocalDateTime getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }
    public String getClinicalNotes() { return clinicalNotes; }
    public void setClinicalNotes(String clinicalNotes) { this.clinicalNotes = clinicalNotes; }
    public String getPrescription() { return prescription; }
    public void setPrescription(String prescription) { this.prescription = prescription; }
    public String getPostVisitSummary() { return postVisitSummary; }
    public void setPostVisitSummary(String postVisitSummary) { this.postVisitSummary = postVisitSummary; }
    public String getFollowUpInstructions() { return followUpInstructions; }
    public void setFollowUpInstructions(String followUpInstructions) { this.followUpInstructions = followUpInstructions; }
    public MedicationFrequency getMedicationFrequency() { return medicationFrequency; }
    public void setMedicationFrequency(MedicationFrequency medicationFrequency) { this.medicationFrequency = medicationFrequency; }
    public LocalDate getMedicationStartDate() { return medicationStartDate; }
    public void setMedicationStartDate(LocalDate medicationStartDate) { this.medicationStartDate = medicationStartDate; }
    public LocalDate getMedicationEndDate() { return medicationEndDate; }
    public void setMedicationEndDate(LocalDate medicationEndDate) { this.medicationEndDate = medicationEndDate; }
    public String getGoogleEventId() { return googleEventId; }
    public void setGoogleEventId(String googleEventId) { this.googleEventId = googleEventId; }
    public Long getCalendarOwnerUserId() { return calendarOwnerUserId; }
    public void setCalendarOwnerUserId(Long calendarOwnerUserId) { this.calendarOwnerUserId = calendarOwnerUserId; }
    public boolean isAppointmentReminderQueued() { return appointmentReminderQueued; }
    public void setAppointmentReminderQueued(boolean appointmentReminderQueued) { this.appointmentReminderQueued = appointmentReminderQueued; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
