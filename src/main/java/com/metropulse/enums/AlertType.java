package com.metropulse.enums;

public enum AlertType {
    SERVICE_DISRUPTION("Service disruption"),
    FACILITY_ISSUE("Station facility issue"),
    ACCESS_RESTRICTION("Temporary access restriction"),
    OTHER("Other station information");

    private final String label;

    AlertType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
