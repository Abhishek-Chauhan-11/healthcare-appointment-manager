package com.healthcare.appointmentmanager;

import com.healthcare.appointmentmanager.model.Appointment;
import com.healthcare.appointmentmanager.model.AppointmentStatus;
import com.healthcare.appointmentmanager.model.DoctorProfile;
import com.healthcare.appointmentmanager.model.NotificationType;
import com.healthcare.appointmentmanager.repository.AppointmentRepository;
import com.healthcare.appointmentmanager.repository.DoctorProfileRepository;
import com.healthcare.appointmentmanager.repository.NotificationJobRepository;
import com.healthcare.appointmentmanager.service.AppointmentBookingService;
import com.healthcare.appointmentmanager.service.BusinessException;
import com.healthcare.appointmentmanager.service.DoctorLeaveService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppointmentBookingServiceTests {

    @Autowired
    private AppointmentBookingService bookingService;

    @Autowired
    private DoctorProfileRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private NotificationJobRepository notificationRepository;

    @Autowired
    private DoctorLeaveService leaveService;

    @Test
    void booksAnAvailableSlotOnTheSelectedDateAndCreatesSafeSummary() {
        DoctorProfile doctor = doctorRepository.findByActiveTrueOrderByIdAsc().get(0);
        LocalDate selectedDate = LocalDate.now().plusDays(2);
        LocalTime time = bookingService.availableSlots(doctor.getId(), selectedDate).get(0);

        Appointment appointment = bookingService.book(
                "patient@healthcare.com", doctor.getId(), selectedDate, time,
                "Mild headache since yesterday");

        assertThat(appointment.getAppointmentDate()).isEqualTo(selectedDate);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
        assertThat(appointment.getReservationKey()).contains(selectedDate.toString());
        assertThat(appointment.getPreVisitSummary()).contains("Chief complaint");
    }

    @Test
    void rejectsASecondBookingForTheSameDoctorDateAndTime() {
        DoctorProfile doctor = doctorRepository.findByActiveTrueOrderByIdAsc().get(0);
        LocalDate selectedDate = LocalDate.now().plusDays(3);
        LocalTime time = bookingService.availableSlots(doctor.getId(), selectedDate).get(0);

        bookingService.book("patient@healthcare.com", doctor.getId(), selectedDate, time,
                "Routine follow-up visit");

        assertThatThrownBy(() -> bookingService.book(
                "patient@healthcare.com", doctor.getId(), selectedDate, time,
                "Another request for the occupied slot"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    void doctorLeaveCancelsBookingsAndQueuesPatientNotification() {
        DoctorProfile doctor = doctorRepository.findByActiveTrueOrderByIdAsc().get(0);
        LocalDate selectedDate = LocalDate.now().plusDays(4);
        LocalTime time = bookingService.availableSlots(doctor.getId(), selectedDate).get(0);
        Appointment appointment = bookingService.book(
                "patient@healthcare.com", doctor.getId(), selectedDate, time,
                "Mild headache for one day");

        int affected = leaveService.addLeave(doctor.getId(), selectedDate, "Training day");

        Appointment cancelled = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertThat(affected).isEqualTo(1);
        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(cancelled.getReservationKey()).isNull();
        assertThat(notificationRepository.findAll())
                .anyMatch(job -> job.getType() == NotificationType.DOCTOR_LEAVE
                        && job.getRecipientEmail().equals("patient@healthcare.com"));
    }
}
