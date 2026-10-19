package com.metropulse.service.routing;

/**
 * routeCost = estimatedTravelTime + crowdPenalty + incidentPenalty + preferencePenalty
 * <p>
 * Here estimatedTravelTime = rideMinutes + transferMinutes and incidentPenalty = delayMinutes.
 * The crowd and transfer penalties are only added to routeCost by the preference that uses them,
 * but crowdPenalty is always calculated so that routes can be compared honestly.
 */
public record CostBreakdown(int rideMinutes,
                            int transferMinutes,
                            int delayMinutes,
                            int crowdPenalty,
                            int transferPenalty,
                            int crowdedStops,
                            int stopsWithCrowdData,
                            double routeCost) {

    public int totalMinutes() {
        return rideMinutes + transferMinutes + delayMinutes;
    }
}
