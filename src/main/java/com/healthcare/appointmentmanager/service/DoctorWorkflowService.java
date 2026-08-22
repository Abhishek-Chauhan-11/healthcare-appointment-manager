package com.healthcare.appointmentmanager.service;

import com.healthcare.appointmentmanager.model.*;
import com.healthcare.appointmentmanager.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DoctorWorkflowService {

    private final AppUserRepository userRepository;
    private final DoctorProfileRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final AiSummaryService aiSummaryService;
    private final MedicationReminderService reminderService;
    private final NotificationService notificationService;

    public DoctorWorkflowService(AppUserRepository userRepository,
                                 DoctorProfileRepository doctorRepository,
                                 AppointmentRepository appointmentRepository,
                                 AiSummaryService aiSummaryService,
                                 MedicationReminderService reminderService,
                                 NotificationService notificationService) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.aiSummaryService = aiSummaryService;
        this.reminderService = reminderService;
        this.notificationService = notificationService;
    }

    public DoctorProfile doctorForEmail(String email) {
        AppUser user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        return doctorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("No doctor profile is linked to this account."));
    }

    public List<Appointment> appointments(String email) {
        DoctorProfile doctor = doctorForEmail(email);
        return appointmentRepository.findByDoctor_IdOrderByAppointmentDateAscStartTimeAsc(doctor.getId()).stream()
                .filter(appointment -> appointment.getStatus() != AppointmentStatus.HELD)
                .toList();
    }

    public Appointment appointment(String email, Long appointmentId) {
        DoctorProfile doctor = doctorForEmail(email);
        return appointmentRepository.findByIdAndDoctor_Id(appointmentId, doctor.getId())
                .orElseThrow(() -> new BusinessException("Appointment not found for this doctor."));
    }

    @Transactional
    public Appointment completeVisit(String doctorEmail, Long appointmentId,
                                     String clinicalNotes, String prescription,
                                     String followUp, MedicationFrequency frequency,
                                     LocalDate medicationStart, LocalDate medicationEnd) {
        DoctorProfile doctor = doctorForEmail(doctorEmail);
        Appointment appointment = appointmentRepository.findByIdAndDoctor_Id(appointmentId, doctor.getId())
                .orElseThrow(() -> new BusinessException("Appointment not found for this doctor."));
        if (appointment.getStatus() != AppointmentStatus.BOOKED &&
                appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BusinessException("A cancelled appointment cannot be completed.");
        }
        if (clinicalNotes == null || clinicalNotes.isBlank()) {
            throw new BusinessException("Clinical notes are required.");
        }
        if (clinicalNotes.length() > 10000 ||
                (prescription != null && prescription.length() > 5000) ||
                (followUp != null && followUp.length() > 5000)) {
            throw new BusinessException("Visit text is too long. Keep notes under 10,000 and other fields under 5,000 characters.");
        }
        MedicationFrequency selectedFrequency = frequency == null ? MedicationFrequency.NONE : frequency;
        if (selectedFrequency != MedicationFrequency.NONE &&
                (prescription == null || prescription.isBlank())) {
            throw new BusinessException("A prescription is required when medication reminders are enabled.");
        }
        if (selectedFrequency != MedicationFrequency.NONE && medicationStart != null &&
                medicationEnd != null && medicationEnd.isBefore(medicationStart)) {
            throw new BusinessException("Medication end date cannot be before its start date.");
        }

        String summary = aiSummaryService.generatePostVisitSummary(clinicalNotes, prescription, followUp);
        appointment.setClinicalNotes(clinicalNotes.trim());
        appointment.setPrescription(prescription == null ? "" : prescription.trim());
        appointment.setFollowUpInstructions(followUp == null ? "" : followUp.trim());
        appointment.setPostVisitSummary(summary);
        appointment.setMedicationFrequency(selectedFrequency);
        appointment.setMedicationStartDate(medicationStart);
        appointment.setMedicationEndDate(medicationEnd);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        Appointment saved = appointmentRepository.save(appointment);
        reminderService.replaceSchedule(saved);
        notificationService.enqueue(saved, NotificationType.VISIT_SUMMARY,
                saved.getPatient().getEmail(), "Your visit summary", summary);
        return saved;
    }
}
