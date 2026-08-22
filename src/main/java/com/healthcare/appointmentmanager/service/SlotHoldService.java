package com.healthcare.appointmentmanager.service;

import com.healthcare.appointmentmanager.model.*;
import com.healthcare.appointmentmanager.repository.AppointmentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class SlotHoldService {

    private final AppointmentRepository repository;

    public SlotHoldService(AppointmentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Appointment createHold(DoctorProfile doctor, AppUser patient, LocalDate date,
                                  LocalTime start, String symptoms) {
        Appointment appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setAppointmentDate(date);
        appointment.setStartTime(start);
        appointment.setEndTime(start.plusMinutes(doctor.getSlotDurationMinutes()));
        appointment.setSymptoms(symptoms.trim());
        appointment.setStatus(AppointmentStatus.HELD);
        appointment.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5));
        appointment.setReservationKey(reservationKey(doctor.getId(), date, start));
        try {
            return repository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException("That appointment slot was just selected by another patient. Please choose another slot.");
        }
    }

    @Transactional
    public Appointment finalizeHold(Long appointmentId, AiSummaryResult summary) {
        Appointment appointment = repository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("Appointment hold no longer exists."));
        if (appointment.getStatus() != AppointmentStatus.HELD ||
                appointment.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("The slot hold expired. Please select the slot again.");
        }
        appointment.setPreVisitSummary(summary.summary());
        appointment.setUrgencyLevel(summary.urgencyLevel());
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setHoldExpiresAt(null);
        return repository.save(appointment);
    }

    @Transactional
    public Appointment cancelForPatient(Long appointmentId, Long patientId) {
        Appointment appointment = repository.findByIdAndPatient_Id(appointmentId, patientId)
                .orElseThrow(() -> new BusinessException("Appointment not found."));
        if (appointment.getStatus() != AppointmentStatus.BOOKED &&
                appointment.getStatus() != AppointmentStatus.HELD) {
            throw new BusinessException("Only an active appointment can be cancelled.");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setReservationKey(null);
        appointment.setHoldExpiresAt(null);
        return repository.saveAndFlush(appointment);
    }

    @Transactional
    public Appointment rescheduleForPatient(Long appointmentId, Long patientId,
                                            LocalDate date, LocalTime start) {
        Appointment appointment = repository.findByIdAndPatient_Id(appointmentId, patientId)
                .orElseThrow(() -> new BusinessException("Appointment not found."));
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new BusinessException("Only a booked appointment can be rescheduled.");
        }
        appointment.setAppointmentDate(date);
        appointment.setStartTime(start);
        appointment.setEndTime(start.plusMinutes(appointment.getDoctor().getSlotDurationMinutes()));
        appointment.setReservationKey(reservationKey(appointment.getDoctor().getId(), date, start));
        appointment.setAppointmentReminderQueued(false);
        try {
            return repository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException("That appointment slot is no longer available.");
        }
    }

    public static String reservationKey(Long doctorId, LocalDate date, LocalTime start) {
        return doctorId + "|" + date + "|" + start.withSecond(0).withNano(0);
    }
}
