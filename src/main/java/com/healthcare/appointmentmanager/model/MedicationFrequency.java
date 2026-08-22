package com.healthcare.appointmentmanager.model;

public enum MedicationFrequency {
    NONE(0),
    ONCE_DAILY(1),
    TWICE_DAILY(2),
    THREE_TIMES_DAILY(3);

    private final int timesPerDay;

    MedicationFrequency(int timesPerDay) {
        this.timesPerDay = timesPerDay;
    }

    public int getTimesPerDay() {
        return timesPerDay;
    }
}
