package com.healthcare.appointmentmanager.repository;

import com.healthcare.appointmentmanager.model.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorProfileRepository
        extends JpaRepository<DoctorProfile, Long> {

    Optional<DoctorProfile> findByUserId(Long userId);

    List<DoctorProfile> findAllByOrderByIdAsc();

    List<DoctorProfile> findByActiveTrueOrderByIdAsc();

    List<DoctorProfile>
    findByActiveTrueAndSpecializationContainingIgnoreCase(
            String specialization);
}
