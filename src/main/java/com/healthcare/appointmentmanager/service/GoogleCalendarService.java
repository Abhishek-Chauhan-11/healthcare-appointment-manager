package com.healthcare.appointmentmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthcare.appointmentmanager.model.AppUser;
import com.healthcare.appointmentmanager.model.Appointment;
import com.healthcare.appointmentmanager.model.GoogleCalendarToken;
import com.healthcare.appointmentmanager.repository.GoogleCalendarTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String EVENTS_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    private static final String SCOPE = "https://www.googleapis.com/auth/calendar.events";

    private final RestClient restClient;
    private final GoogleCalendarTokenRepository tokenRepository;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final ZoneId zoneId;

    public GoogleCalendarService(
            RestClient.Builder builder,
            GoogleCalendarTokenRepository tokenRepository,
            @Value("${app.google.client-id:}") String clientId,
            @Value("${app.google.client-secret:}") String clientSecret,
            @Value("${app.google.redirect-uri:http://localhost:8080/calendar/callback}") String redirectUri,
            @Value("${app.time-zone:Asia/Kolkata}") String timeZone) {
        this.restClient = builder.build();
        this.tokenRepository = tokenRepository;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.zoneId = ZoneId.of(timeZone);
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    public boolean isConnected(Long userId) {
        return tokenRepository.findByUser_Id(userId).isPresent();
    }

    public String authorizationUrl(String state) {
        if (!isConfigured()) throw new IllegalStateException("Google Calendar OAuth is not configured");
        return UriComponentsBuilder.fromUriString(AUTH_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPE)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("include_granted_scopes", "true")
                .queryParam("state", state)
                .build().encode().toUriString();
    }

    @Transactional
    public void exchangeAuthorizationCode(AppUser user, String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", redirectUri);

        JsonNode response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
        if (response == null || response.path("access_token").asText().isBlank()) {
            throw new IllegalStateException("Google did not return an access token");
        }

        GoogleCalendarToken token = tokenRepository.findByUser_Id(user.getId())
                .orElseGet(GoogleCalendarToken::new);
        token.setUser(user);
        token.setAccessToken(response.path("access_token").asText());
        if (!response.path("refresh_token").asText().isBlank()) {
            token.setRefreshToken(response.path("refresh_token").asText());
        }
        token.setExpiresAt(LocalDateTime.now().plusSeconds(response.path("expires_in").asLong(3600)));
        token.setScope(response.path("scope").asText(SCOPE));
        tokenRepository.save(token);
    }

    @Transactional
    public void disconnect(Long userId) {
        tokenRepository.findByUser_Id(userId).ifPresent(tokenRepository::delete);
    }

    public void createEventIfConnected(Appointment appointment) {
        try {
            GoogleCalendarToken token = tokenRepository.findByUser_Id(appointment.getPatient().getId()).orElse(null);
            if (token == null) return;
            JsonNode response = restClient.post()
                    .uri(EVENTS_URL + "?sendUpdates=all")
                    .header("Authorization", "Bearer " + validAccessToken(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(eventPayload(appointment))
                    .retrieve().body(JsonNode.class);
            if (response != null && !response.path("id").asText().isBlank()) {
                appointment.setGoogleEventId(response.path("id").asText());
                appointment.setCalendarOwnerUserId(appointment.getPatient().getId());
            }
        } catch (Exception exception) {
            log.warn("Calendar event creation failed without breaking booking: {}", exception.getMessage());
        }
    }

    public void updateEventIfConnected(Appointment appointment) {
        if (appointment.getGoogleEventId() == null || appointment.getCalendarOwnerUserId() == null) return;
        try {
            GoogleCalendarToken token = tokenRepository.findByUser_Id(appointment.getCalendarOwnerUserId()).orElse(null);
            if (token == null) return;
            String eventId = UriUtils.encodePathSegment(appointment.getGoogleEventId(), StandardCharsets.UTF_8);
            restClient.put()
                    .uri(EVENTS_URL + "/" + eventId + "?sendUpdates=all")
                    .header("Authorization", "Bearer " + validAccessToken(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(eventPayload(appointment))
                    .retrieve().toBodilessEntity();
        } catch (Exception exception) {
            log.warn("Calendar event update failed: {}", exception.getMessage());
        }
    }

    public void deleteEventIfConnected(Appointment appointment) {
        if (appointment.getGoogleEventId() == null || appointment.getCalendarOwnerUserId() == null) return;
        try {
            GoogleCalendarToken token = tokenRepository.findByUser_Id(appointment.getCalendarOwnerUserId()).orElse(null);
            if (token == null) return;
            String eventId = UriUtils.encodePathSegment(appointment.getGoogleEventId(), StandardCharsets.UTF_8);
            restClient.delete()
                    .uri(EVENTS_URL + "/" + eventId + "?sendUpdates=all")
                    .header("Authorization", "Bearer " + validAccessToken(token))
                    .retrieve().toBodilessEntity();
        } catch (Exception exception) {
            log.warn("Calendar event deletion failed: {}", exception.getMessage());
        }
    }

    private String validAccessToken(GoogleCalendarToken token) {
        if (token.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(1))) return token.getAccessToken();
        if (token.getRefreshToken() == null || token.getRefreshToken().isBlank()) {
            throw new IllegalStateException("Calendar authorization expired; reconnect Google Calendar");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", token.getRefreshToken());
        form.add("grant_type", "refresh_token");
        JsonNode response = restClient.post().uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form).retrieve().body(JsonNode.class);
        if (response == null || response.path("access_token").asText().isBlank()) {
            throw new IllegalStateException("Unable to refresh Google Calendar token");
        }
        token.setAccessToken(response.path("access_token").asText());
        token.setExpiresAt(LocalDateTime.now().plusSeconds(response.path("expires_in").asLong(3600)));
        tokenRepository.save(token);
        return token.getAccessToken();
    }

    private Map<String, Object> eventPayload(Appointment appointment) {
        String start = appointment.getAppointmentDate().atTime(appointment.getStartTime())
                .atZone(zoneId).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String end = appointment.getAppointmentDate().atTime(appointment.getEndTime())
                .atZone(zoneId).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return Map.of(
                "summary", "Medical appointment with " + appointment.getDoctor().getUser().getFullName(),
                "description", "Healthcare Appointment Manager booking. Do not place medical symptoms in calendar descriptions.",
                "start", Map.of("dateTime", start, "timeZone", zoneId.getId()),
                "end", Map.of("dateTime", end, "timeZone", zoneId.getId()),
                "attendees", List.of(
                        Map.of("email", appointment.getPatient().getEmail()),
                        Map.of("email", appointment.getDoctor().getUser().getEmail())
                )
        );
    }
}
