package com.metropulse.enums;

public enum FacilityStatus {
    AVAILABLE("Available"), OUT_OF_SERVICE("Out of service"), UNKNOWN("Status unknown");

    private final String label;

    FacilityStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
