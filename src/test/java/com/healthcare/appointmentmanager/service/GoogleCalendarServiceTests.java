package com.healthcare.appointmentmanager.service;

import com.healthcare.appointmentmanager.model.AppUser;
import com.healthcare.appointmentmanager.model.Appointment;
import com.healthcare.appointmentmanager.model.DoctorProfile;
import com.healthcare.appointmentmanager.model.GoogleCalendarToken;
import com.healthcare.appointmentmanager.repository.GoogleCalendarTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleCalendarServiceTests {

    @Test
    void createsEventsForConnectedPatientAndDoctorCalendars() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GoogleCalendarTokenRepository repository = mock(GoogleCalendarTokenRepository.class);
        AppUser patient = mock(AppUser.class);
        AppUser doctorUser = mock(AppUser.class);
        DoctorProfile doctor = mock(DoctorProfile.class);
        when(patient.getId()).thenReturn(10L);
        when(doctorUser.getId()).thenReturn(20L);
        when(doctorUser.getFullName()).thenReturn("Dr. Demo");
        when(doctor.getUser()).thenReturn(doctorUser);
        when(repository.findByUser_Id(10L)).thenReturn(Optional.of(token("patient-token")));
        when(repository.findByUser_Id(20L)).thenReturn(Optional.of(token("doctor-token")));

        server.expect(requestTo("https://www.googleapis.com/calendar/v3/calendars/primary/events?sendUpdates=none"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer patient-token"))
                .andRespond(withSuccess("{\"id\":\"patient-event\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://www.googleapis.com/calendar/v3/calendars/primary/events?sendUpdates=none"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer doctor-token"))
                .andRespond(withSuccess("{\"id\":\"doctor-event\"}", MediaType.APPLICATION_JSON));

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(LocalDate.now().plusDays(1));
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 30));
        GoogleCalendarService service = new GoogleCalendarService(
                builder, repository, "client-id", "client-secret", "http://localhost/callback", "Asia/Kolkata");

        service.createEventIfConnected(appointment);

        assertEquals("patient-event", appointment.getGoogleEventId());
        assertEquals(10L, appointment.getCalendarOwnerUserId());
        assertEquals("doctor-event", appointment.getDoctorGoogleEventId());
        assertEquals(20L, appointment.getDoctorCalendarOwnerUserId());
        server.verify();
    }

    private GoogleCalendarToken token(String accessToken) {
        GoogleCalendarToken token = new GoogleCalendarToken();
        token.setAccessToken(accessToken);
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        return token;
    }
}
