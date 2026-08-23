package com.healthcare.appointmentmanager;

import com.healthcare.appointmentmanager.model.AppUser;
import com.healthcare.appointmentmanager.repository.AppUserRepository;
import com.healthcare.appointmentmanager.repository.DoctorProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class AppointmentManagerApplicationTests {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Test
    void demoDoctorProfileIsLinkedToDoctorAccount() {
        AppUser doctor = appUserRepository
                .findByEmailIgnoreCase("doctor@healthcare.com")
                .orElseThrow();
        AppUser admin = appUserRepository
                .findByEmailIgnoreCase("admin@healthcare.com")
                .orElseThrow();

        assertTrue(doctorProfileRepository.findByUserId(doctor.getId()).isPresent());
        assertTrue(doctorProfileRepository.findByUserId(admin.getId()).isEmpty());
    }
}
