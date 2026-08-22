package com.healthcare.appointmentmanager.service;

import com.healthcare.appointmentmanager.model.UrgencyLevel;

public record AiSummaryResult(String summary, UrgencyLevel urgencyLevel, boolean generatedByLlm) {
}
