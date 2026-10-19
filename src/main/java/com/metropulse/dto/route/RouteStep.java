package com.metropulse.dto.route;

/** One step of a journey in travel order: either a ride on a line or a transfer. Exactly one is set. */
public record RouteStep(RouteLeg ride, RouteTransfer transfer) {

    public static RouteStep of(RouteLeg ride) {
        return new RouteStep(ride, null);
    }

    public static RouteStep of(RouteTransfer transfer) {
        return new RouteStep(null, transfer);
    }

    public boolean isRideStep() {
        return ride != null;
    }
}
