package com.healthcare.appointmentmanager.controller;

import com.healthcare.appointmentmanager.model.Appointment;
import com.healthcare.appointmentmanager.model.DoctorProfile;
import com.healthcare.appointmentmanager.repository.AppointmentRepository;
import com.healthcare.appointmentmanager.repository.DoctorProfileRepository;
import com.healthcare.appointmentmanager.service.AppointmentBookingService;
import com.healthcare.appointmentmanager.service.DoctorWorkflowService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PortalApiController {

    private final DoctorProfileRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentBookingService bookingService;
    private final DoctorWorkflowService workflowService;

    public PortalApiController(DoctorProfileRepository doctorRepository,
                               AppointmentRepository appointmentRepository,
                               AppointmentBookingService bookingService,
                               DoctorWorkflowService workflowService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.bookingService = bookingService;
        this.workflowService = workflowService;
    }

    @GetMapping("/patient/doctors")
    public List<DoctorView> doctors(@RequestParam(required = false) String specialization) {
        List<DoctorProfile> doctors = specialization == null || specialization.isBlank()
                ? doctorRepository.findByActiveTrueOrderByIdAsc()
                : doctorRepository.findByActiveTrueAndSpecializationContainingIgnoreCase(specialization.trim());
        return doctors.stream().map(DoctorView::from).toList();
    }

    @GetMapping("/patient/doctors/{doctorId}/slots")
    public List<LocalTime> slots(@PathVariable Long doctorId,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.availableSlots(doctorId, date);
    }

    @GetMapping("/patient/appointments")
    public List<AppointmentView> patientAppointments(Authentication authentication) {
        return bookingService.patientAppointments(authentication.getName()).stream()
                .map(AppointmentView::from).toList();
    }

    @GetMapping("/doctor/appointments")
    public List<AppointmentView> doctorAppointments(Authentication authentication) {
        return workflowService.appointments(authentication.getName()).stream()
                .map(AppointmentView::from).toList();
    }

    @GetMapping("/admin/appointments")
    public List<OperationalAppointmentView> allAppointments() {
        return appointmentRepository.findAllByOrderByAppointmentDateDescStartTimeDesc().stream()
                .map(OperationalAppointmentView::from).toList();
    }

    public record DoctorView(Long id, String name, String specialization, String qualification,
                             Integer experienceYears, BigDecimal consultationFee,
                             LocalTime workingStart, LocalTime workingEnd, Integer slotMinutes) {
        static DoctorView from(DoctorProfile doctor) {
            return new DoctorView(doctor.getId(), doctor.getUser().getFullName(), doctor.getSpecialization(),
                    doctor.getQualification(), doctor.getExperienceYears(), doctor.getConsultationFee(),
                    doctor.getWorkingStartTime(), doctor.getWorkingEndTime(), doctor.getSlotDurationMinutes());
        }
    }

    public record AppointmentView(Long id, String doctor, String patient, LocalDate date,
                                  LocalTime start, LocalTime end, String status, String urgency,
                                  String preVisitSummary, String postVisitSummary, String prescription,
                                  String followUpInstructions) {
        static AppointmentView from(Appointment appointment) {
            return new AppointmentView(appointment.getId(), appointment.getDoctor().getUser().getFullName(),
                    appointment.getPatient().getFullName(), appointment.getAppointmentDate(),
                    appointment.getStartTime(), appointment.getEndTime(), appointment.getStatus().name(),
                    appointment.getUrgencyLevel().name(), appointment.getPreVisitSummary(),
                    appointment.getPostVisitSummary(), appointment.getPrescription(),
                    appointment.getFollowUpInstructions());
        }
    }

    public record OperationalAppointmentView(Long id, String doctor, String patient,
                                             LocalDate date, LocalTime start, LocalTime end,
                                             String status, String urgency) {
        static OperationalAppointmentView from(Appointment appointment) {
            return new OperationalAppointmentView(appointment.getId(),
                    appointment.getDoctor().getUser().getFullName(),
                    appointment.getPatient().getFullName(), appointment.getAppointmentDate(),
                    appointment.getStartTime(), appointment.getEndTime(),
                    appointment.getStatus().name(), appointment.getUrgencyLevel().name());
        }
    }
}
