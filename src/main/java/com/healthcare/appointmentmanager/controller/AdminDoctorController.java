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
    public String showDoctorForm(Model model) {
        model.addAttribute("editing", false);
        return "admin/doctor-form";
    }

    @GetMapping("/{doctorId}/edit")
    public String showEditDoctorForm(@PathVariable Long doctorId, Model model) {
        DoctorProfile doctor = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found."));
        model.addAttribute("doctor", doctor);
        model.addAttribute("editing", true);
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

        String validationError = validateProfile(fullName, specialization, experienceYears,
                consultationFee, workingStartTime, workingEndTime, slotDurationMinutes);
        if (validationError != null) {
            model.addAttribute("error", validationError);
            model.addAttribute("editing", false);
            return "admin/doctor-form";
        }

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            model.addAttribute(
                    "error",
                    "An account with this email already exists."
            );
            return "admin/doctor-form";
        }

        if (!email.contains("@")) {
            model.addAttribute("error", "Enter a valid email address.");
            model.addAttribute("editing", false);
            return "admin/doctor-form";
        }

        if (password.length() < 8) {
            model.addAttribute(
                    "error",
                    "Password must contain at least 8 characters."
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

    @PostMapping("/{doctorId}")
    @Transactional
    public String updateDoctor(
            @PathVariable Long doctorId,
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam String specialization,
            @RequestParam String qualification,
            @RequestParam Integer experienceYears,
            @RequestParam BigDecimal consultationFee,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime workingStartTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime workingEndTime,
            @RequestParam Integer slotDurationMinutes,
            @RequestParam(defaultValue = "false") boolean active,
            Model model) {

        DoctorProfile profile = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found."));
        String validationError = validateProfile(fullName, specialization, experienceYears,
                consultationFee, workingStartTime, workingEndTime, slotDurationMinutes);
        if (validationError != null) {
            model.addAttribute("error", validationError);
            model.addAttribute("doctor", profile);
            model.addAttribute("editing", true);
            return "admin/doctor-form";
        }

        profile.getUser().setFullName(fullName.trim());
        profile.getUser().setPhone(phone.trim());
        profile.setSpecialization(specialization.trim());
        profile.setQualification(qualification.trim());
        profile.setExperienceYears(experienceYears);
        profile.setConsultationFee(consultationFee);
        profile.setWorkingStartTime(workingStartTime);
        profile.setWorkingEndTime(workingEndTime);
        profile.setSlotDurationMinutes(slotDurationMinutes);
        profile.setActive(active);
        doctorProfileRepository.save(profile);

        return "redirect:/admin/doctors?updated";
    }

    private String validateProfile(String fullName, String specialization, Integer experienceYears,
                                   BigDecimal consultationFee, LocalTime workingStartTime,
                                   LocalTime workingEndTime, Integer slotDurationMinutes) {
        if (fullName == null || fullName.isBlank() || specialization == null || specialization.isBlank()) {
            return "Doctor name and specialization are required.";
        }
        if (experienceYears == null || experienceYears < 0 || experienceYears > 80) {
            return "Experience must be between 0 and 80 years.";
        }
        if (consultationFee == null || consultationFee.signum() < 0) {
            return "Consultation fee cannot be negative.";
        }
        if (workingStartTime == null || workingEndTime == null || !workingStartTime.isBefore(workingEndTime)) {
            return "Working start time must be before end time.";
        }
        if (slotDurationMinutes == null || slotDurationMinutes < 5 || slotDurationMinutes > 240) {
            return "Slot duration must be between 5 and 240 minutes.";
        }
        return null;
    }
}
