package com.healthcare.appointmentmanager.controller;

import com.healthcare.appointmentmanager.model.AppUser;
import com.healthcare.appointmentmanager.model.DoctorProfile;
import com.healthcare.appointmentmanager.model.Role;
import com.healthcare.appointmentmanager.repository.AppUserRepository;
import com.healthcare.appointmentmanager.repository.DoctorProfileRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Controller
@RequestMapping("/admin/doctors")
public class AdminDoctorController {

    private final AppUserRepository appUserRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminDoctorController(
            AppUserRepository appUserRepository,
            DoctorProfileRepository doctorProfileRepository,
            PasswordEncoder passwordEncoder) {

        this.appUserRepository = appUserRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String listDoctors(Model model) {
        model.addAttribute(
                "doctors",
                doctorProfileRepository.findAllByOrderByIdAsc()
        );

        return "admin/doctor-list";
    }

    @GetMapping("/new")
    public String showDoctorForm() {
        return "admin/doctor-form";
    }

    @PostMapping
    @Transactional
    public String createDoctor(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String password,
            @RequestParam String specialization,
            @RequestParam String qualification,
            @RequestParam Integer experienceYears,
            @RequestParam BigDecimal consultationFee,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            LocalTime workingStartTime,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            LocalTime workingEndTime,
            @RequestParam Integer slotDurationMinutes,
            Model model) {

        email = email.trim().toLowerCase();

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            model.addAttribute(
                    "error",
                    "An account with this email already exists."
            );
            return "admin/doctor-form";
        }

        if (password.length() < 8) {
            model.addAttribute(
                    "error",
                    "Password must contain at least 8 characters."
            );
            return "admin/doctor-form";
        }

        if (!workingStartTime.isBefore(workingEndTime)) {
            model.addAttribute(
                    "error",
                    "Working start time must be before end time."
            );
            return "admin/doctor-form";
        }

        AppUser doctorUser = new AppUser(
                fullName.trim(),
                email,
                passwordEncoder.encode(password),
                phone.trim(),
                Role.DOCTOR
        );

        AppUser savedUser = appUserRepository.save(doctorUser);

        DoctorProfile profile = new DoctorProfile();
        profile.setUser(savedUser);
        profile.setSpecialization(specialization.trim());
        profile.setQualification(qualification.trim());
        profile.setExperienceYears(experienceYears);
        profile.setConsultationFee(consultationFee);
        profile.setWorkingStartTime(workingStartTime);
        profile.setWorkingEndTime(workingEndTime);
        profile.setSlotDurationMinutes(slotDurationMinutes);
        profile.setActive(true);

        doctorProfileRepository.save(profile);

        return "redirect:/admin/doctors?created";
    }
}
