package com.healthcare.appointmentmanager;

import com.healthcare.appointmentmanager.model.Appointment;
import com.healthcare.appointmentmanager.model.AppointmentStatus;
import com.healthcare.appointmentmanager.model.DoctorProfile;
import com.healthcare.appointmentmanager.repository.DoctorProfileRepository;
import com.healthcare.appointmentmanager.service.AppointmentBookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppointmentBookingServiceTests {

    @Autowired
    private AppointmentBookingService bookingService;

    @Autowired
    private DoctorProfileRepository doctorRepository;

    @Test
    void booksAnAvailableSlotAndCreatesSafeSummaryWithoutExternalKeys() {
        DoctorProfile doctor = doctorRepository.findByActiveTrueOrderByIdAsc().getFirst();
        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime time = bookingService.availableSlots(doctor.getId(), date).getFirst();

        Appointment appointment = bookingService.book(
                "patient@healthcare.com", doctor.getId(), date, time,
                "Mild headache since yesterday");

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
        assertThat(appointment.getReservationKey()).isNotBlank();
        assertThat(appointment.getPreVisitSummary()).contains("Chief complaint");
    }
}
