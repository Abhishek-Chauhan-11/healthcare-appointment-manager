package com.healthcare.appointmentmanager.config;

import com.healthcare.appointmentmanager.model.AppUser;
import com.healthcare.appointmentmanager.model.Role;
import com.healthcare.appointmentmanager.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner createDemoUsers(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            createUserIfMissing(
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
        };
    }

    private void createUserIfMissing(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            String fullName,
            String email,
            String password,
            String phone,
            Role role) {

        if (!appUserRepository.existsByEmailIgnoreCase(email)) {
            AppUser user = new AppUser(
                    fullName,
                    email,
                    passwordEncoder.encode(password),
                    phone,
                    role
            );

            appUserRepository.save(user);
        }
    }
}