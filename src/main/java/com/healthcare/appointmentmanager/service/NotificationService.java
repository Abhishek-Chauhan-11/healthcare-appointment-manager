package com.healthcare.appointmentmanager.service;

import com.healthcare.appointmentmanager.model.*;
import com.healthcare.appointmentmanager.repository.NotificationJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationJobRepository repository;
    private final SendGridEmailGateway emailGateway;

    public NotificationService(NotificationJobRepository repository, SendGridEmailGateway emailGateway) {
        this.repository = repository;
        this.emailGateway = emailGateway;
    }

    @Transactional
    public void enqueue(Appointment appointment, NotificationType type, String email, String subject, String body) {
        if (email == null || email.isBlank()) return;
        NotificationJob job = new NotificationJob();
        job.setAppointment(appointment);
        job.setType(type);
        job.setRecipientEmail(email);
        job.setSubject(subject);
        job.setBody(body);
        job.setStatus(NotificationStatus.PENDING);
        job.setNextAttemptAt(LocalDateTime.now());
        repository.save(job);
    }

    @Scheduled(fixedDelayString = "${app.jobs.notification-delay-ms:60000}")
    @Transactional
    public void processOutbox() {
        List<NotificationJob> jobs = repository
                .findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        List.of(NotificationStatus.PENDING, NotificationStatus.FAILED), LocalDateTime.now());

        for (NotificationJob job : jobs) {
            if (!emailGateway.isConfigured()) {
                job.setStatus(NotificationStatus.SKIPPED);
                job.setLastError("SendGrid is not configured; notification retained for audit");
                continue;
            }
            try {
                emailGateway.send(job.getRecipientEmail(), job.getSubject(), job.getBody());
                job.setStatus(NotificationStatus.SENT);
                job.setLastError(null);
            } catch (Exception exception) {
                int attempts = job.getAttempts() + 1;
                job.setAttempts(attempts);
                job.setLastError(limit(exception.getMessage()));
                if (attempts >= 3) {
                    job.setStatus(NotificationStatus.SKIPPED);
                    log.error("Notification permanently failed for {}: {}", job.getRecipientEmail(), exception.getMessage());
                } else {
                    job.setStatus(NotificationStatus.FAILED);
                    job.setNextAttemptAt(LocalDateTime.now().plusMinutes(attempts * 5L));
                }
            }
        }
    }

    private String limit(String message) {
        if (message == null) return "Unknown email error";
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
