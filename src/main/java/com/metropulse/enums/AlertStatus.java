package com.metropulse.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Alert workflow: DRAFT -> ACTIVE -> RESOLVED, and DRAFT/ACTIVE -> CANCELLED.
 * RESOLVED and CANCELLED are final.
 */
public enum AlertStatus {
    DRAFT, ACTIVE, RESOLVED, CANCELLED;

    public Set<AlertStatus> allowedNext() {
        return switch (this) {
            case DRAFT -> EnumSet.of(ACTIVE, CANCELLED);
            case ACTIVE -> EnumSet.of(RESOLVED, CANCELLED);
            case RESOLVED, CANCELLED -> EnumSet.noneOf(AlertStatus.class);
        };
    }

    public boolean canTransitionTo(AlertStatus target) {
        return allowedNext().contains(target);
    }

    public boolean isEditable() {
        return this == DRAFT || this == ACTIVE;
    }
}
