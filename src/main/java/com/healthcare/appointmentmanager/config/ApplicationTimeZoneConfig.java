package com.healthcare.appointmentmanager.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class ApplicationTimeZoneConfig {

    private final ZoneId zoneId;

    public ApplicationTimeZoneConfig(
            @Value("${app.time-zone:Asia/Kolkata}") String timeZone) {
        this.zoneId = ZoneId.of(timeZone);
    }

    @PostConstruct
    public void configureApplicationTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
    }
}
