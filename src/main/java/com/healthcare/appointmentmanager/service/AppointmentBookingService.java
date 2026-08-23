package com.healthcare.appointmentmanager.service;

import com.healthcare.appointmentmanager.model.*;
import com.healthcare.appointmentmanager.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AppointmentBookingService {

    private final DoctorProfileRepository doctorRepository;
    private final AppUserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorLeaveRepository leaveRepository;
    private final SlotHoldService slotHoldService;
    private final AiSummaryService aiSummaryService;
    private final NotificationService notificationService;
    private final GoogleCalendarService calendarService;

    public AppointmentBookingService(DoctorProfileRepository doctorRepository,
                                     AppUserRepository userRepository,
                                     AppointmentRepository appointmentRepository,
                                     DoctorLeaveRepository leaveRepository,
                                     SlotHoldService slotHoldService,
                                     AiSummaryService aiSummaryService,
                                     NotificationService notificationService,
                                     GoogleCalendarService calendarService) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.leaveRepository = leaveRepository;
        this.slotHoldService = slotHoldService;
        this.aiSummaryService = aiSummaryService;
        this.notificationService = notificationService;
        this.calendarService = calendarService;
    }

    public Appointment book(String patientEmail, Long doctorId, LocalDate date,
                            LocalTime start, String symptoms) {
        AppUser patient = userRepository.findByEmailIgnoreCase(patientEmail).orElseThrow();
        DoctorProfile doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("Doctor not found."));
        validateSlot(doctor, date, start);
        if (symptoms == null || symptoms.isBlank()) {
            throw new BusinessException("Please describe the symptoms before booking.");
        }
        if (symptoms.length() > 3000) {
            throw new BusinessException("Symptoms must be 3,000 characters or fewer.");
        }

        Appointment hold = slotHoldService.createHold(doctor, patient, date, start, symptoms);
        AiSummaryResult summary = aiSummaryService.generatePreVisitSummary(symptoms);
        Appointment booked = slotHoldService.finalizeHold(hold.getId(), summary);

        calendarService.createEventIfConnected(booked);
        appointmentRepository.save(booked);
        queueBookingNotifications(booked);
        return booked;
    }

    public List<LocalTime> availableSlots(Long doctorId, LocalDate date) {
        DoctorProfile doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("Doctor not found."));
        if (!doctor.isActive() || date == null || date.isBefore(LocalDate.now()) ||
                leaveRepository.existsByDoctor_IdAndLeaveDate(doctorId, date)) return List.of();

        List<Appointment> reservations = appointmentRepository
                .findByDoctor_IdAndAppointmentDateAndStatusIn(
                        doctorId, date, List.of(
                                AppointmentStatus.HELD,
                                AppointmentStatus.BOOKED,
                                AppointmentStatus.COMPLETED));
        List<LocalTime> reserved = reservations.stream()
                .map(Appointment::getStartTime).toList();

        List<LocalTime> result = new ArrayList<>();
        LocalTime slot = doctor.getWorkingStartTime();
        while (!slot.plusMinutes(doctor.getSlotDurationMinutes()).isAfter(doctor.getWorkingEndTime())) {
            boolean future = !date.equals(LocalDate.now()) || slot.isAfter(LocalTime.now());
            if (future && !reserved.contains(slot)) result.add(slot);
            slot = slot.plusMinutes(doctor.getSlotDurationMinutes());
        }
        return result;
    }

    public Appointment cancel(String patientEmail, Long appointmentId) {
        AppUser patient = userRepository.findByEmailIgnoreCase(patientEmail).orElseThrow();
        Appointment cancelled = slotHoldService.cancelForPatient(appointmentId, patient.getId());
        calendarService.deleteEventIfConnected(cancelled);
        String body = appointmentDetails("Appointment cancelled", cancelled);
        notificationService.enqueue(cancelled, NotificationType.CANCELLATION,
                cancelled.getPatient().getEmail(), "Appointment cancelled", body);
        notificationService.enqueue(cancelled, NotificationType.CANCELLATION,
                cancelled.getDoctor().getUser().getEmail(), "Appointment cancelled", body);
        return cancelled;
    }

    public Appointment reschedule(String patientEmail, Long appointmentId, LocalDate date, LocalTime start) {
        AppUser patient = userRepository.findByEmailIgnoreCase(patientEmail).orElseThrow();
        Appointment existing = appointmentRepository.findByIdAndPatient_Id(appointmentId, patient.getId())
                .orElseThrow(() -> new BusinessException("Appointment not found."));
        validateSlot(existing.getDoctor(), date, start);
        Appointment changed = slotHoldService.rescheduleForPatient(appointmentId, patient.getId(), date, start);
        calendarService.updateEventIfConnected(changed);
        String body = appointmentDetails("Appointment rescheduled", changed);
        notificationService.enqueue(changed, NotificationType.RESCHEDULE_CONFIRMATION,
                changed.getPatient().getEmail(), "Appointment rescheduled", body);
        notificationService.enqueue(changed, NotificationType.RESCHEDULE_CONFIRMATION,
                changed.getDoctor().getUser().getEmail(), "Appointment rescheduled", body);
        return changed;
    }

    public List<Appointment> patientAppointments(String email) {
        AppUser patient = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        return appointmentRepository.findByPatient_IdOrderByAppointmentDateDescStartTimeDesc(patient.getId());
    }

    public Appointment patientAppointment(String email, Long appointmentId) {
        AppUser patient = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        return appointmentRepository.findByIdAndPatient_Id(appointmentId, patient.getId())
                .orElseThrow(() -> new BusinessException("Appointment not found."));
    }

    private void validateSlot(DoctorProfile doctor, LocalDate date, LocalTime start) {
        if (!doctor.isActive()) throw new BusinessException("This doctor is not accepting appointments.");
        if (date == null || date.isBefore(LocalDate.now())) throw new BusinessException("Choose today or a future date.");
        if (leaveRepository.existsByDoctor_IdAndLeaveDate(doctor.getId(), date)) {
            throw new BusinessException("The doctor is on leave on the selected date.");
        }
        if (!availableSlots(doctor.getId(), date).contains(start)) {
            throw new BusinessException("The selected time is outside working hours or already booked.");
        }
    }

    private void queueBookingNotifications(Appointment appointment) {
        String body = appointmentDetails("Appointment confirmed", appointment);
        notificationService.enqueue(appointment, NotificationType.BOOKING_CONFIRMATION,
                appointment.getPatient().getEmail(), "Appointment confirmation", body);
        notificationService.enqueue(appointment, NotificationType.BOOKING_CONFIRMATION,
                appointment.getDoctor().getUser().getEmail(), "New patient appointment", body);
    }

    private String appointmentDetails(String heading, Appointment appointment) {
        return heading + "\nDoctor: " + appointment.getDoctor().getUser().getFullName() +
                "\nPatient: " + appointment.getPatient().getFullName() +
                "\nDate: " + appointment.getAppointmentDate() +
                "\nTime: " + appointment.getStartTime() + " - " + appointment.getEndTime();
    }

    @Scheduled(fixedDelayString = "${app.jobs.hold-cleanup-delay-ms:60000}")
    @Transactional
    public void removeExpiredHolds() {
        appointmentRepository.deleteAll(
                appointmentRepository.findByStatusAndHoldExpiresAtBefore(
                        AppointmentStatus.HELD, LocalDateTime.now()));
    }

    @Scheduled(cron = "${app.jobs.appointment-reminder-cron:0 0 * * * *}")
    @Transactional
    public void queueAppointmentReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        for (Appointment appointment : appointmentRepository
                .findByAppointmentDateAndStatus(tomorrow, AppointmentStatus.BOOKED)) {
            if (appointment.isAppointmentReminderQueued()) continue;
            String body = appointmentDetails("Appointment reminder", appointment);
            notificationService.enqueue(appointment, NotificationType.APPOINTMENT_REMINDER,
                    appointment.getPatient().getEmail(), "Appointment reminder", body);
            notificationService.enqueue(appointment, NotificationType.APPOINTMENT_REMINDER,
                    appointment.getDoctor().getUser().getEmail(), "Appointment reminder", body);
            appointment.setAppointmentReminderQueued(true);
        }
    }
}
