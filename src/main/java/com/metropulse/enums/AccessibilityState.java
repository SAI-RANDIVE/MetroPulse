package com.metropulse.enums;

/** Step-free status derived from recorded facilities. UNKNOWN never means "accessible". */
public enum AccessibilityState {
    STEP_FREE_RECORDED("Working lift or wheelchair access recorded"),
    NOT_STEP_FREE("Recorded step-free facilities are out of service"),
    UNKNOWN("No step-free facility data");

    private final String label;

    AccessibilityState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
