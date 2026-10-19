package com.metropulse.enums;

public enum AlertSeverity {
    INFO("Information"), WARNING("Warning"), CRITICAL("Critical");

    private final String label;

    AlertSeverity(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
