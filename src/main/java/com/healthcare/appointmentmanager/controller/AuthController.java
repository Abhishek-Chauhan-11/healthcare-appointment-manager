package com.healthcare.appointmentmanager.controller;

import com.healthcare.appointmentmanager.model.AppUser;
import com.healthcare.appointmentmanager.model.Role;
import com.healthcare.appointmentmanager.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AppUserRepository appUserRepository,
                          PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerPatient(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model) {

        fullName = fullName.trim();
        email = email.trim().toLowerCase();
        phone = phone.trim();

        model.addAttribute("fullName", fullName);
        model.addAttribute("email", email);
        model.addAttribute("phone", phone);

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            model.addAttribute("error", "Required fields cannot be empty.");
            return "register";
        }

        if (!email.contains("@")) {
            model.addAttribute("error", "Enter a valid email address.");
            return "register";
        }

        if (password.length() < 8) {
            model.addAttribute("error",
                    "Password must contain at least 8 characters.");
            return "register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "register";
        }

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            model.addAttribute("error",
                    "An account with this email already exists.");
            return "register";
        }

        AppUser patient = new AppUser(
                fullName,
                email,
                passwordEncoder.encode(password),
                phone,
                Role.PATIENT
        );

        appUserRepository.save(patient);

        return "redirect:/login?registered";
    }
}