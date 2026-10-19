package com.metropulse.dto;

import com.metropulse.enums.AccessibilityState;

import java.util.List;

/** Live status of a station: GET /api/stations/{id}/status and part of the station page. */
public record StationStatusView(StationRef station, List<CrowdStatusView> crowd, List<AlertSummary> activeAlerts,
                                AccessibilityState accessibility, List<FacilityView> facilities) {
}
