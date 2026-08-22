package com.healthcare.appointmentmanager.repository;

import com.healthcare.appointmentmanager.model.DoctorLeave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DoctorLeaveRepository extends JpaRepository<DoctorLeave, Long> {
    boolean existsByDoctor_IdAndLeaveDate(Long doctorId, LocalDate leaveDate);
    Optional<DoctorLeave> findByDoctor_IdAndLeaveDate(Long doctorId, LocalDate leaveDate);
    List<DoctorLeave> findAllByOrderByLeaveDateDesc();
}
