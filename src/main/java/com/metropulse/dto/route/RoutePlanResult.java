package com.metropulse.dto.route;

import com.metropulse.dto.AlertNotice;
import com.metropulse.dto.StationRef;
import com.metropulse.enums.RoutePreference;

import java.time.Instant;
import java.util.List;

/** Everything shown on the route-results page and returned by GET /api/routes. */
public record RoutePlanResult(StationRef origin,
                              StationRef destination,
                              RoutePreference requestedPreference,
                              boolean stepFreeRequested,
                              List<RouteOption> options,
                              int distinctRouteCount,
                              boolean crowdDataAvailable,
                              List<AlertNotice> alertsConsidered,
                              List<String> notes,
                              Instant calculatedAt) {

    /** The option that matches the passenger's chosen preference. */
    public RouteOption recommended() {
        return options.stream().filter(RouteOption::recommended).findFirst().orElse(options.getFirst());
    }
}
