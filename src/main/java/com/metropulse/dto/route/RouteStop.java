package com.metropulse.dto.route;

import com.metropulse.enums.CrowdLevel;

/** A stop on a leg. {@code crowdLevel} is null when there is no current report - never assumed LOW. */
public record RouteStop(long stationId, String stationName, long lineStationId, CrowdLevel crowdLevel) {
}
