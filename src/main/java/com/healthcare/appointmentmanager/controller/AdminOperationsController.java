package com.healthcare.appointmentmanager.controller;

import com.healthcare.appointmentmanager.repository.AppointmentRepository;
import com.healthcare.appointmentmanager.repository.DoctorLeaveRepository;
import com.healthcare.appointmentmanager.repository.DoctorProfileRepository;
import com.healthcare.appointmentmanager.service.BusinessException;
import com.healthcare.appointmentmanager.service.DoctorLeaveService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class AdminOperationsController {

    private final DoctorProfileRepository doctorRepository;
    private final DoctorLeaveRepository leaveRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorLeaveService leaveService;

    public AdminOperationsController(DoctorProfileRepository doctorRepository,
                                     DoctorLeaveRepository leaveRepository,
                                     AppointmentRepository appointmentRepository,
                                     DoctorLeaveService leaveService) {
        this.doctorRepository = doctorRepository;
        this.leaveRepository = leaveRepository;
        this.appointmentRepository = appointmentRepository;
        this.leaveService = leaveService;
    }

    @GetMapping("/admin/leaves")
    public String leaves(Model model) {
        model.addAttribute("doctors", doctorRepository.findAllByOrderByIdAsc());
        model.addAttribute("leaves", leaveRepository.findAllByOrderByLeaveDateDesc());
        model.addAttribute("minimumDate", LocalDate.now());
        return "admin/leaves";
    }

    @PostMapping("/admin/leaves")
    public String addLeave(@RequestParam Long doctorId,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam(required = false) String reason,
                           RedirectAttributes redirectAttributes) {
        try {
            int affected = leaveService.addLeave(doctorId, date, reason);
            redirectAttributes.addFlashAttribute("success",
                    "Leave saved. " + affected + " appointment(s) were cancelled.");
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/leaves";
    }

    @PostMapping("/admin/leaves/{leaveId}/delete")
    public String deleteLeave(@PathVariable Long leaveId, RedirectAttributes redirectAttributes) {
        leaveService.removeLeave(leaveId);
        redirectAttributes.addFlashAttribute("success", "Leave removed.");
        return "redirect:/admin/leaves";
    }

    @GetMapping("/admin/appointments")
    public String appointments(Model model) {
        model.addAttribute("appointments", appointmentRepository.findAllByOrderByAppointmentDateDescStartTimeDesc());
        return "admin/appointments";
    }
}
