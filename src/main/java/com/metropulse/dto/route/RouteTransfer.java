package com.metropulse.dto.route;

/** A change between lines, possibly walking to a neighbouring station (e.g. D N Nagar to Andheri (West)). */
public record RouteTransfer(String fromStation, String fromLineCode, String toStation, String toLineCode,
                            int minutes, String note) {

    public boolean isWalkBetweenStations() {
        return !fromStation.equals(toStation);
    }
}
