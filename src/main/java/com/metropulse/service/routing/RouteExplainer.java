package com.metropulse.service.routing;

import com.metropulse.enums.RoutePreference;

/**
 * Writes the one-paragraph "why this route" text. It only claims what the data supports:
 * with no current crowd reports it says crowding is unknown instead of calling a route "less crowded".
 */
public final class RouteExplainer {

    private RouteExplainer() {
    }

    /**
     * @param option       cost breakdown of the route being explained
     * @param fastest      cost breakdown of the fastest route (for comparison)
     * @param sameAsFastest whether this route is identical to the fastest one
     * @param avoidedAlert title of a verified alert whose suspended connection the fastest route avoids, or null
     */
    public static String explain(RoutePreference preference, CostBreakdown option, CostBreakdown fastest,
                                 long interchanges, long fastestInterchanges, boolean sameAsFastest,
                                 boolean crowdDataAvailable, int transferPenaltyMinutes, String avoidedAlert) {
        String text = switch (preference) {
            case FASTEST -> "Fastest based on configured estimated travel times.";
            case LESS_CROWDED -> lessCrowded(option, fastest, sameAsFastest, crowdDataAvailable);
            case FEWER_TRANSFERS -> fewerTransfers(option, fastest, interchanges, fastestInterchanges, sameAsFastest,
                    transferPenaltyMinutes);
        };
        if (option.delayMinutes() > 0) {
            text += " Includes about " + option.delayMinutes() + " min of delay reported in verified service alerts.";
        }
        if (avoidedAlert != null) {
            text += " Avoids a connection that is suspended by the verified alert \"" + avoidedAlert + "\".";
        }
        return text;
    }

    private static String lessCrowded(CostBreakdown option, CostBreakdown fastest, boolean sameAsFastest,
                                      boolean crowdDataAvailable) {
        if (!crowdDataAvailable) {
            return "There are no current crowd reports on the network, so crowding is unknown and this is simply the fastest route.";
        }
        if (sameAsFastest) {
            if (option.stopsWithCrowdData() == 0) {
                return "No current crowd reports exist for stops on this route, so its crowding is unknown; no better-reported alternative was found.";
            }
            return "The fastest route already has the lowest reported crowding of the routes considered.";
        }
        int extra = option.totalMinutes() - fastest.totalMinutes();
        return "Recommended because it has fewer reported crowding issues (crowd penalty " + option.crowdPenalty()
                + " vs " + fastest.crowdPenalty() + " on the fastest route)"
                + (extra > 0 ? ", for about " + extra + " extra minutes." : ".");
    }

    private static String fewerTransfers(CostBreakdown option, CostBreakdown fastest, long interchanges,
                                         long fastestInterchanges, boolean sameAsFastest, int penalty) {
        if (sameAsFastest) {
            return interchanges == 0
                    ? "No interchange is needed - stay on the same line."
                    : "No route with fewer interchanges is worth the extra time (each interchange is weighted as +" + penalty + " min).";
        }
        int extra = option.totalMinutes() - fastest.totalMinutes();
        return "Uses " + plural(interchanges, "interchange") + " instead of " + fastestInterchanges
                + (extra > 0 ? ", for about " + extra + " extra minutes." : ".");
    }

    public static String plural(long count, String word) {
        return count + " " + word + (count == 1 ? "" : "s");
    }
}
