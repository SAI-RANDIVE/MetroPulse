package com.metropulse.enums;

public enum CrowdLevel {
    LOW("Low", "Plenty of space"),
    MODERATE("Moderate", "Busy but comfortable"),
    HIGH("High", "Crowded platforms or trains"),
    VERY_HIGH("Very high", "Packed - expect to wait");

    private final String label;
    private final String hint;

    CrowdLevel(String label, String hint) {
        this.label = label;
        this.hint = hint;
    }

    public String getLabel() {
        return label;
    }

    public String getHint() {
        return hint;
    }

    public String getCssClass() {
        return "crowd-" + name().toLowerCase().replace('_', '-');
    }
}
