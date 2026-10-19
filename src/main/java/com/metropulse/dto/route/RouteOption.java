package com.metropulse.dto.route;

import com.metropulse.enums.RoutePreference;

import java.util.List;

/**
 * One recommendation. When two preferences produce the same path, the later one has
 * {@code sameRouteAs} set and is shown as a duplicate, not as a separate alternative.
 */
public record RouteOption(RoutePreference preference,
                          String label,
                          boolean recommended,
                          RoutePreference sameRouteAs,
                          List<RouteStep> steps,
                          int stationCount,
                          int rideMinutes,
                          int transferMinutes,
                          int delayMinutes,
                          int totalMinutes,
                          int interchanges,
                          double routeCost,
                          int crowdPenalty,
                          int crowdedStops,
                          int stopsWithCrowdData,
                          String explanation,
                          List<String> warnings) {

    public boolean isDuplicate() {
        return sameRouteAs != null;
    }

    public List<RouteLeg> legs() {
        return steps.stream().filter(RouteStep::isRideStep).map(RouteStep::ride).toList();
    }

    public List<RouteTransfer> transfers() {
        return steps.stream().filter(s -> !s.isRideStep()).map(RouteStep::transfer).toList();
    }
}
