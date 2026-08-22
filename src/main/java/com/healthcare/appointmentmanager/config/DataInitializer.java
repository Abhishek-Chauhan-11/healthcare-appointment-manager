package com.healthcare.appointmentmanager.config;

import com.healthcare.appointmentmanager.model.AppUser;
import com.healthcare.appointmentmanager.model.DoctorProfile;
import com.healthcare.appointmentmanager.model.Role;
import com.healthcare.appointmentmanager.repository.AppUserRepository;
import com.healthcare.appointmentmanager.repository.DoctorProfileRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalTime;

@Configuration
public class DataInitializer {

    @Bean
    @ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true", matchIfMissing = true)
    public CommandLineRunner createDemoUsers(
            AppUserRepository appUserRepository,
            DoctorProfileRepository doctorProfileRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            AppUser doctor = createUserIfMissing(
                    appUserRepository,
                    passwordEncoder,
                    "System Administrator",
                    "admin@healthcare.com",
                    "Admin@123",
                    "9999999999",
                    Role.ADMIN
            );

            createUserIfMissing(
                    appUserRepository,
                    passwordEncoder,
                    "Dr. Demo",
                    "doctor@healthcare.com",
                    "Doctor@123",
                    "8888888888",
                    Role.DOCTOR
            );

            createUserIfMissing(
                    appUserRepository,
                    passwordEncoder,
                    "Demo Patient",
                    "patient@healthcare.com",
                    "Patient@123",
                    "7777777777",
                    Role.PATIENT
            );

            if (doctorProfileRepository.findByUserId(doctor.getId()).isEmpty()) {
                DoctorProfile profile = new DoctorProfile();
                profile.setUser(doctor);
                profile.setSpecialization("General Medicine");
                profile.setQualification("MBBS, MD");
                profile.setExperienceYears(8);
                profile.setConsultationFee(new BigDecimal("500.00"));
                profile.setWorkingStartTime(LocalTime.of(9, 0));
                profile.setWorkingEndTime(LocalTime.of(17, 0));
                profile.setSlotDurationMinutes(30);
                profile.setActive(true);
                doctorProfileRepository.save(profile);
            }
        };
    }

    private AppUser createUserIfMissing(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            String fullName,
            String email,
            String password,
            String phone,
            Role role) {

        return appUserRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            AppUser user = new AppUser(
                    fullName,
                    email,
                    passwordEncoder.encode(password),
                    phone,
                    role
            );
            return appUserRepository.save(user);
        });
    }
}
