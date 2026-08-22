package com.healthcare.appointmentmanager.repository;

import com.healthcare.appointmentmanager.model.Appointment;
import com.healthcare.appointmentmanager.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatient_IdOrderByAppointmentDateDescStartTimeDesc(Long patientId);
    List<Appointment> findByDoctor_IdOrderByAppointmentDateAscStartTimeAsc(Long doctorId);
    List<Appointment> findAllByOrderByAppointmentDateDescStartTimeDesc();
    Optional<Appointment> findByIdAndPatient_Id(Long id, Long patientId);
    Optional<Appointment> findByIdAndDoctor_Id(Long id, Long doctorId);
    List<Appointment> findByDoctor_IdAndAppointmentDateAndStatusIn(
            Long doctorId, LocalDate date, Collection<AppointmentStatus> statuses);
    List<Appointment> findByAppointmentDateAndStatus(LocalDate date, AppointmentStatus status);
    List<Appointment> findByStatusAndHoldExpiresAtBefore(AppointmentStatus status, java.time.LocalDateTime cutoff);
}
