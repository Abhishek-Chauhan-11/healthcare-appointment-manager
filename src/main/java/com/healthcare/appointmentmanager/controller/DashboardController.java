package com.healthcare.appointmentmanager.controller;

import com.healthcare.appointmentmanager.model.AppUser;
import com.healthcare.appointmentmanager.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final AppUserRepository appUserRepository;

    public DashboardController(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/dashboard")
    public String redirectToDashboard(Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        boolean isDoctor = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_DOCTOR"));

        if (isAdmin) {
            return "redirect:/admin/dashboard";
        }

        if (isDoctor) {
            return "redirect:/doctor/dashboard";
        }

        return "redirect:/patient/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Authentication authentication, Model model) {
        addUserDetails(authentication, model, "ADMIN", "Admin Portal");
        return "dashboard";
    }

    @GetMapping("/doctor/dashboard")
    public String doctorDashboard(Authentication authentication, Model model) {
        addUserDetails(authentication, model, "DOCTOR", "Doctor Portal");
        return "dashboard";
    }

    @GetMapping("/patient/dashboard")
    public String patientDashboard(Authentication authentication, Model model) {
        addUserDetails(authentication, model, "PATIENT", "Patient Portal");
        return "dashboard";
    }

    private void addUserDetails(
            Authentication authentication,
            Model model,
            String portal,
            String title) {

        AppUser user = appUserRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow();

        model.addAttribute("user", user);
        model.addAttribute("portal", portal);
        model.addAttribute("title", title);
    }
}