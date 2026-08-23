package com.healthcare.appointmentmanager.service;

import com.healthcare.appointmentmanager.model.NotificationJob;
import com.healthcare.appointmentmanager.model.NotificationStatus;
import com.healthcare.appointmentmanager.model.NotificationType;
import com.healthcare.appointmentmanager.repository.NotificationJobRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTests {

    @Test
    void retainsAnAuditRecordWhenEmailIsNotConfigured() {
        NotificationJobRepository repository = mock(NotificationJobRepository.class);
        SendGridEmailGateway gateway = mock(SendGridEmailGateway.class);
        NotificationJob job = pendingJob();
        when(repository.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any(), any()))
                .thenReturn(List.of(job));
        when(gateway.isConfigured()).thenReturn(false);

        new NotificationService(repository, gateway).processOutbox();

        assertEquals(NotificationStatus.SKIPPED, job.getStatus());
        assertTrue(job.getLastError().contains("not configured"));
    }

    @Test
    void retriesProviderFailuresAndStopsAfterThreeAttempts() {
        NotificationJobRepository repository = mock(NotificationJobRepository.class);
        SendGridEmailGateway gateway = mock(SendGridEmailGateway.class);
        NotificationJob job = pendingJob();
        when(repository.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any(), any()))
                .thenReturn(List.of(job));
        when(gateway.isConfigured()).thenReturn(true);
        doThrow(new IllegalStateException("temporary provider failure"))
                .when(gateway).send(any(), any(), any());
        NotificationService service = new NotificationService(repository, gateway);

        service.processOutbox();
        assertEquals(NotificationStatus.FAILED, job.getStatus());
        assertEquals(1, job.getAttempts());
        service.processOutbox();
        service.processOutbox();

        assertEquals(NotificationStatus.SKIPPED, job.getStatus());
        assertEquals(3, job.getAttempts());
        verify(gateway, org.mockito.Mockito.times(3)).send(any(), any(), any());
    }

    private NotificationJob pendingJob() {
        NotificationJob job = new NotificationJob();
        job.setType(NotificationType.BOOKING_CONFIRMATION);
        job.setRecipientEmail("patient@example.com");
        job.setSubject("Appointment confirmation");
        job.setBody("Appointment booked");
        job.setStatus(NotificationStatus.PENDING);
        job.setNextAttemptAt(LocalDateTime.now().minusMinutes(1));
        return job;
    }
}
