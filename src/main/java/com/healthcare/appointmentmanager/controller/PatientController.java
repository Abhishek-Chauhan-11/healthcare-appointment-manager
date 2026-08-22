package com.healthcare.appointmentmanager.controller;

import com.healthcare.appointmentmanager.model.Appointment;
import com.healthcare.appointmentmanager.model.AppointmentStatus;
import com.healthcare.appointmentmanager.model.DoctorProfile;
import com.healthcare.appointmentmanager.repository.DoctorProfileRepository;
import com.healthcare.appointmentmanager.service.AppointmentBookingService;
import com.healthcare.appointmentmanager.service.BusinessException;
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
import java.time.LocalTime;
import java.util.List;

@Controller
public class PatientController {

    private final DoctorProfileRepository doctorRepository;
    private final AppointmentBookingService bookingService;

    public PatientController(DoctorProfileRepository doctorRepository,
                             AppointmentBookingService bookingService) {
        this.doctorRepository = doctorRepository;
        this.bookingService = bookingService;
    }

    @GetMapping("/patient/doctors")
    public String doctors(@RequestParam(required = false) String specialization, Model model) {
        List<DoctorProfile> doctors = specialization == null || specialization.isBlank()
                ? doctorRepository.findByActiveTrueOrderByIdAsc()
                : doctorRepository.findByActiveTrueAndSpecializationContainingIgnoreCase(specialization.trim());
        model.addAttribute("doctors", doctors);
        model.addAttribute("specialization", specialization == null ? "" : specialization);
        return "patient/doctors";
    }

    @GetMapping("/patient/doctors/{doctorId}/book")
    public String bookingForm(@PathVariable Long doctorId,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              Model model) {
        DoctorProfile doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("Doctor not found."));
        LocalDate selectedDate = date == null ? LocalDate.now().plusDays(1) : date;
        model.addAttribute("doctor", doctor);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("minimumDate", LocalDate.now());
        model.addAttribute("slots", bookingService.availableSlots(doctorId, selectedDate));
        return "patient/book";
    }

    @PostMapping("/patient/appointments")
    public String book(Authentication authentication,
                       @RequestParam Long doctorId,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                       @RequestParam String symptoms,
                       RedirectAttributes redirectAttributes) {
        try {
            Appointment appointment = bookingService.book(
                    authentication.getName(), doctorId, date, startTime, symptoms);
            redirectAttributes.addFlashAttribute("success",
                    "Appointment #" + appointment.getId() + " is confirmed.");
            return "redirect:/patient/appointments";
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/patient/doctors/" + doctorId + "/book?date=" + date;
        }
    }

    @GetMapping("/patient/appointments")
    public String appointments(Authentication authentication, Model model) {
        model.addAttribute("appointments", bookingService.patientAppointments(authentication.getName()));
        return "patient/appointments";
    }

    @PostMapping("/patient/appointments/{appointmentId}/cancel")
    public String cancel(Authentication authentication, @PathVariable Long appointmentId,
                         RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancel(authentication.getName(), appointmentId);
            redirectAttributes.addFlashAttribute("success", "Appointment cancelled.");
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/patient/appointments";
    }

    @GetMapping("/patient/appointments/{appointmentId}/reschedule")
    public String rescheduleForm(Authentication authentication, @PathVariable Long appointmentId,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 Model model) {
        Appointment appointment = bookingService.patientAppointment(authentication.getName(), appointmentId);
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new BusinessException("Only confirmed appointments can be rescheduled.");
        }
        LocalDate selectedDate = date == null ? appointment.getAppointmentDate() : date;
        model.addAttribute("appointment", appointment);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("minimumDate", LocalDate.now());
        model.addAttribute("slots", bookingService.availableSlots(appointment.getDoctor().getId(), selectedDate));
        return "patient/reschedule";
    }

    @PostMapping("/patient/appointments/{appointmentId}/reschedule")
    public String reschedule(Authentication authentication, @PathVariable Long appointmentId,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                             RedirectAttributes redirectAttributes) {
        try {
            bookingService.reschedule(authentication.getName(), appointmentId, date, startTime);
            redirectAttributes.addFlashAttribute("success", "Appointment rescheduled.");
            return "redirect:/patient/appointments";
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/patient/appointments/" + appointmentId + "/reschedule?date=" + date;
        }
    }
}
