package com.metropulse.enums;

/** How an active alert affects route planning. */
public enum AlertImpact {
    /** Shown to commuters only; routes are unchanged. */
    INFORMATION("Information only"),
    /** Adds the alert's delay minutes to the affected connections. */
    DELAY("Delay on affected connections"),
    /** Affected connections are removed from the routing graph. */
    SUSPENDED("Affected connections unavailable");

    private final String label;

    AlertImpact(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
