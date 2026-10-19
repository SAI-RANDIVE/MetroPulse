package com.metropulse.enums;

public enum Role {
    COMMUTER, ADMIN;

    /** Spring Security authority name, e.g. ROLE_ADMIN. */
    public String authority() {
        return "ROLE_" + name();
    }
}
