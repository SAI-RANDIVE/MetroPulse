package com.metropulse.dto;

/** A service alert that changed route costs or availability for a journey. */
public record AlertNotice(long id, String title, String effect) {
}
