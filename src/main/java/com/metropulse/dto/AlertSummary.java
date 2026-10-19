package com.metropulse.dto;

import com.metropulse.entity.ServiceAlert;
import com.metropulse.enums.*;

import java.time.Instant;

public record AlertSummary(long id, String title, String description, AlertType type, AlertSeverity severity,
                           AlertImpact impact, int delayMinutes, AlertStatus status,
                           LineBadge line, StationRef station, int affectedConnections,
                           Instant startsAt, Instant endsAt, Instant verifiedAt, Instant updatedAt, boolean demo) {

    /** Requires line, station and affectedEdges to be loaded. */
    public static AlertSummary of(ServiceAlert a) {
        return new AlertSummary(a.getId(), a.getTitle(), a.getDescription(), a.getAlertType(), a.getSeverity(),
                a.getImpact(), a.getDelayMinutes(), a.getStatus(),
                a.getLine() == null ? null : LineBadge.of(a.getLine()),
                a.getStation() == null ? null : new StationRef(a.getStation().getId(), a.getStation().getName()),
                a.getAffectedEdges().size(), a.getStartsAt(), a.getEndsAt(), a.getVerifiedAt(), a.getUpdatedAt(), a.isDemo());
    }

    /** Variant for lists where the affected edges were not fetched. */
    public static AlertSummary withoutEdges(ServiceAlert a) {
        return new AlertSummary(a.getId(), a.getTitle(), a.getDescription(), a.getAlertType(), a.getSeverity(),
                a.getImpact(), a.getDelayMinutes(), a.getStatus(),
                a.getLine() == null ? null : LineBadge.of(a.getLine()),
                a.getStation() == null ? null : new StationRef(a.getStation().getId(), a.getStation().getName()),
                -1, a.getStartsAt(), a.getEndsAt(), a.getVerifiedAt(), a.getUpdatedAt(), a.isDemo());
    }
}
