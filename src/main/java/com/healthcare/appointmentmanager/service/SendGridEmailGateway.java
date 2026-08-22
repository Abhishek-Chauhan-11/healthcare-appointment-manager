package com.healthcare.appointmentmanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class SendGridEmailGateway {

    private final RestClient restClient;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    public SendGridEmailGateway(
            RestClient.Builder builder,
            @Value("${app.sendgrid.api-key:}") String apiKey,
            @Value("${app.sendgrid.from-email:}") String fromEmail,
            @Value("${app.sendgrid.from-name:Healthcare Appointment Manager}") String fromName) {
        this.restClient = builder.build();
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && fromEmail != null && !fromEmail.isBlank();
    }

    public void send(String recipient, String subject, String body) {
        if (!isConfigured()) throw new IllegalStateException("SendGrid is not configured");

        Map<String, Object> payload = Map.of(
                "personalizations", List.of(Map.of("to", List.of(Map.of("email", recipient)))),
                "from", Map.of("email", fromEmail, "name", fromName),
                "subject", subject,
                "content", List.of(Map.of("type", "text/plain", "value", body))
        );

        restClient.post()
                .uri("https://api.sendgrid.com/v3/mail/send")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
