package com.metropulse.enums;

public enum FacilityType {
    LIFT("Lift"),
    ESCALATOR("Escalator"),
    WHEELCHAIR_ACCESS("Wheelchair access"),
    ACCESSIBLE_ENTRANCE("Accessible entrance"),
    RESTROOM("Restroom"),
    PARKING("Parking"),
    OTHER("Other");

    private final String label;

    FacilityType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Facilities that provide step-free movement between street, concourse and platform. */
    public boolean isStepFree() {
        return this == LIFT || this == WHEELCHAIR_ACCESS;
    }
}
