package com.healthcare.appointmentmanager.service;

import com.healthcare.appointmentmanager.model.*;
import com.healthcare.appointmentmanager.repository.AppointmentRepository;
import com.healthcare.appointmentmanager.repository.DoctorLeaveRepository;
import com.healthcare.appointmentmanager.repository.DoctorProfileRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DoctorLeaveService {

    private final DoctorProfileRepository doctorRepository;
    private final DoctorLeaveRepository leaveRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;
    private final GoogleCalendarService calendarService;

    public DoctorLeaveService(DoctorProfileRepository doctorRepository,
                              DoctorLeaveRepository leaveRepository,
                              AppointmentRepository appointmentRepository,
                              NotificationService notificationService,
                              GoogleCalendarService calendarService) {
        this.doctorRepository = doctorRepository;
        this.leaveRepository = leaveRepository;
        this.appointmentRepository = appointmentRepository;
        this.notificationService = notificationService;
        this.calendarService = calendarService;
    }

    @Transactional
    public int addLeave(Long doctorId, LocalDate date, String reason) {
        if (date.isBefore(LocalDate.now())) throw new BusinessException("Leave cannot be added in the past.");
        DoctorProfile doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("Doctor not found."));
        DoctorLeave leave = new DoctorLeave();
        leave.setDoctor(doctor);
        leave.setLeaveDate(date);
        leave.setReason(reason == null || reason.isBlank() ? "Unavailable" : reason.trim());
        try {
            leaveRepository.saveAndFlush(leave);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException("Leave is already recorded for this doctor and date.");
        }

        List<Appointment> affected = appointmentRepository
                .findByDoctor_IdAndAppointmentDateAndStatusIn(
                        doctorId, date, List.of(AppointmentStatus.HELD, AppointmentStatus.BOOKED));
        for (Appointment appointment : affected) {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointment.setReservationKey(null);
            appointment.setHoldExpiresAt(null);
            calendarService.deleteEventIfConnected(appointment);
            String body = "The appointment on " + date + " at " + appointment.getStartTime() +
                    " was cancelled because " + doctor.getUser().getFullName() + " is unavailable. Please book another slot.";
            notificationService.enqueue(appointment, NotificationType.DOCTOR_LEAVE,
                    appointment.getPatient().getEmail(), "Appointment affected by doctor leave", body);
        }
        return affected.size();
    }

    @Transactional
    public void removeLeave(Long leaveId) {
        leaveRepository.deleteById(leaveId);
    }
}
