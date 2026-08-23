package com.healthcare.appointmentmanager.controller;

import com.healthcare.appointmentmanager.model.AppUser;
import com.healthcare.appointmentmanager.model.DoctorProfile;
import com.healthcare.appointmentmanager.model.Role;
import com.healthcare.appointmentmanager.repository.AppUserRepository;
import com.healthcare.appointmentmanager.repository.DoctorProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDoctorControllerTests {

    @Test
    void updatesDoctorProfileAndAvailability() {
        AppUserRepository users = mock(AppUserRepository.class);
        DoctorProfileRepository doctors = mock(DoctorProfileRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        DoctorProfile profile = profile();
        when(doctors.findById(1L)).thenReturn(Optional.of(profile));
        AdminDoctorController controller = new AdminDoctorController(users, doctors, encoder);

        String result = controller.updateDoctor(
                1L, "Dr. Updated", "9876543210", "Neurology", "MBBS, MD",
                12, new BigDecimal("750.00"), LocalTime.of(10, 0),
                LocalTime.of(16, 0), 30, false, new ExtendedModelMap());

        assertEquals("redirect:/admin/doctors?updated", result);
        assertEquals("Dr. Updated", profile.getUser().getFullName());
        assertEquals("Neurology", profile.getSpecialization());
        assertFalse(profile.isActive());
        verify(doctors).save(profile);
    }

    private DoctorProfile profile() {
        AppUser user = new AppUser(
                "Dr. Demo", "doctor@example.com", "encoded-password", "1234567890", Role.DOCTOR);
        DoctorProfile profile = new DoctorProfile();
        profile.setUser(user);
        profile.setSpecialization("General Medicine");
        profile.setQualification("MBBS");
        profile.setExperienceYears(8);
        profile.setConsultationFee(new BigDecimal("500.00"));
        profile.setWorkingStartTime(LocalTime.of(9, 0));
        profile.setWorkingEndTime(LocalTime.of(17, 0));
        profile.setSlotDurationMinutes(30);
        profile.setActive(true);
        return profile;
    }
}
