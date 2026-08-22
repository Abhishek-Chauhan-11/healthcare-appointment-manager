package com.healthcare.appointmentmanager.repository;

import com.healthcare.appointmentmanager.model.GoogleCalendarToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleCalendarTokenRepository extends JpaRepository<GoogleCalendarToken, Long> {
    Optional<GoogleCalendarToken> findByUser_Id(Long userId);
}
