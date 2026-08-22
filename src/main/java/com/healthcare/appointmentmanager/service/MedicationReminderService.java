package com.healthcare.appointmentmanager.service;

import com.healthcare.appointmentmanager.model.*;
import com.healthcare.appointmentmanager.repository.MedicationReminderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class MedicationReminderService {

    private final MedicationReminderRepository repository;
    private final NotificationService notificationService;

    public MedicationReminderService(MedicationReminderRepository repository,
                                     NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void replaceSchedule(Appointment appointment) {
        repository.deleteByAppointment_Id(appointment.getId());
        if (appointment.getMedicationFrequency() == null ||
                appointment.getMedicationFrequency() == MedicationFrequency.NONE ||
                appointment.getPrescription() == null || appointment.getPrescription().isBlank()) return;

        LocalDate start = appointment.getMedicationStartDate() == null
                ? LocalDate.now() : appointment.getMedicationStartDate();
        LocalDate end = appointment.getMedicationEndDate() == null
                ? start.plusDays(6) : appointment.getMedicationEndDate();
        if (end.isBefore(start)) end = start;
        if (end.isAfter(start.plusDays(89))) end = start.plusDays(89);

        List<LocalTime> times = switch (appointment.getMedicationFrequency()) {
            case ONCE_DAILY -> List.of(LocalTime.of(9, 0));
            case TWICE_DAILY -> List.of(LocalTime.of(9, 0), LocalTime.of(20, 0));
            case THREE_TIMES_DAILY -> List.of(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0));
            case NONE -> List.of();
        };

        LocalDate date = start;
        while (!date.isAfter(end)) {
            for (LocalTime time : times) {
                LocalDateTime scheduled = LocalDateTime.of(date, time);
                if (scheduled.isAfter(LocalDateTime.now())) {
                    MedicationReminder reminder = new MedicationReminder();
                    reminder.setAppointment(appointment);
                    reminder.setPatient(appointment.getPatient());
                    reminder.setScheduledFor(scheduled);
                    reminder.setMedicationText(appointment.getPrescription());
                    repository.save(reminder);
                }
            }
            date = date.plusDays(1);
        }
    }

    @Scheduled(fixedDelayString = "${app.jobs.medication-delay-ms:60000}")
    @Transactional
    public void queueDueReminders() {
        List<MedicationReminder> reminders = repository
                .findTop50ByStatusInAndScheduledForLessThanEqualOrderByScheduledForAsc(
                        List.of(ReminderStatus.PENDING, ReminderStatus.FAILED), LocalDateTime.now());
        for (MedicationReminder reminder : reminders) {
            try {
                notificationService.enqueue(
                        reminder.getAppointment(),
                        NotificationType.MEDICATION_REMINDER,
                        reminder.getPatient().getEmail(),
                        "Medication reminder",
                        "Medication instructions:\n" + reminder.getMedicationText());
                reminder.setStatus(ReminderStatus.SENT);
            } catch (Exception exception) {
                reminder.setAttempts(reminder.getAttempts() + 1);
                reminder.setStatus(reminder.getAttempts() >= 3 ? ReminderStatus.SKIPPED : ReminderStatus.FAILED);
                reminder.setLastError(exception.getMessage());
            }
        }
    }
}
