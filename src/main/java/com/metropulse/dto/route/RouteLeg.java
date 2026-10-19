package com.metropulse.dto.route;

import java.util.List;

/** A continuous ride on one line. {@code stops} includes the boarding and alighting stations. */
public record RouteLeg(String lineCode,
                       String lineName,
                       String lineColor,
                       String lineTextColor,
                       String towards,
                       List<RouteStop> stops,
                       int minutes) {

    public RouteStop boarding() {
        return stops.getFirst();
    }

    public RouteStop alighting() {
        return stops.getLast();
    }

    public int hops() {
        return stops.size() - 1;
    }
}
