package com.metropulse.enums;

/** What MetroPulse knows about crowding at a station on a line right now. */
public enum CrowdInfoState {
    VERIFIED("Verified by an administrator"),
    USER_REPORTED("Reported by a commuter (not yet verified)"),
    STALE("Last report has expired"),
    NO_DATA("No recent reports");

    private final String label;

    CrowdInfoState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
