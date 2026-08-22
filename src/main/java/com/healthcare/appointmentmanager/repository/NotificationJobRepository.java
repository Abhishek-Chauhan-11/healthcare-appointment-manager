package com.healthcare.appointmentmanager.repository;

import com.healthcare.appointmentmanager.model.NotificationJob;
import com.healthcare.appointmentmanager.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface NotificationJobRepository extends JpaRepository<NotificationJob, Long> {
    List<NotificationJob> findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            Collection<NotificationStatus> statuses, LocalDateTime now);
}
