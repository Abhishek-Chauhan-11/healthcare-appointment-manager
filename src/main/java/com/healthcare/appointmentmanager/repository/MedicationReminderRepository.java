package com.healthcare.appointmentmanager.repository;

import com.healthcare.appointmentmanager.model.MedicationReminder;
import com.healthcare.appointmentmanager.model.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MedicationReminderRepository extends JpaRepository<MedicationReminder, Long> {
    List<MedicationReminder> findTop50ByStatusInAndScheduledForLessThanEqualOrderByScheduledForAsc(
            Collection<ReminderStatus> statuses, LocalDateTime now);
    void deleteByAppointment_Id(Long appointmentId);
}
