package com.metropulse.enums;

public enum RoutePreference {
    FASTEST("Fastest"),
    LESS_CROWDED("Less crowded"),
    FEWER_TRANSFERS("Fewer transfers");

    private final String label;

    RoutePreference(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
