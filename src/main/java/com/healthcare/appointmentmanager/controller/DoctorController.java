package com.healthcare.appointmentmanager.controller;

import com.healthcare.appointmentmanager.model.Appointment;
import com.healthcare.appointmentmanager.model.MedicationFrequency;
import com.healthcare.appointmentmanager.service.BusinessException;
import com.healthcare.appointmentmanager.service.DoctorWorkflowService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class DoctorController {

    private final DoctorWorkflowService workflowService;

    public DoctorController(DoctorWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/doctor/appointments")
    public String appointments(Authentication authentication, Model model) {
        model.addAttribute("appointments", workflowService.appointments(authentication.getName()));
        return "doctor/appointments";
    }

    @GetMapping("/doctor/appointments/{appointmentId}")
    public String details(Authentication authentication, @PathVariable Long appointmentId, Model model) {
        model.addAttribute("appointment", workflowService.appointment(authentication.getName(), appointmentId));
        return "doctor/appointment-details";
    }

    @GetMapping("/doctor/appointments/{appointmentId}/visit")
    public String visitForm(Authentication authentication, @PathVariable Long appointmentId, Model model) {
        model.addAttribute("appointment", workflowService.appointment(authentication.getName(), appointmentId));
        model.addAttribute("frequencies", MedicationFrequency.values());
        return "doctor/visit-form";
    }

    @PostMapping("/doctor/appointments/{appointmentId}/visit")
    public String completeVisit(Authentication authentication, @PathVariable Long appointmentId,
                                @RequestParam String clinicalNotes,
                                @RequestParam(required = false) String prescription,
                                @RequestParam(required = false) String followUp,
                                @RequestParam(defaultValue = "NONE") MedicationFrequency frequency,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate medicationStart,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate medicationEnd,
                                RedirectAttributes redirectAttributes) {
        try {
            Appointment appointment = workflowService.completeVisit(authentication.getName(), appointmentId,
                    clinicalNotes, prescription, followUp, frequency, medicationStart, medicationEnd);
            redirectAttributes.addFlashAttribute("success", "Visit notes saved and patient summary created.");
            return "redirect:/doctor/appointments/" + appointment.getId();
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/doctor/appointments/" + appointmentId + "/visit";
        }
    }
}
