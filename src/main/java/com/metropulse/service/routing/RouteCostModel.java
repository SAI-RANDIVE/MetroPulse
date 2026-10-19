package com.metropulse.service.routing;

import com.metropulse.config.MetroPulseProperties;
import com.metropulse.enums.CrowdLevel;
import com.metropulse.enums.RoutePreference;
import org.springframework.stereotype.Component;

/**
 * Turns a route preference plus the current {@link RoutingConditions} into edge weights.
 * <ul>
 *     <li>FASTEST: estimated minutes + alert delays.</li>
 *     <li>LESS_CROWDED: as FASTEST + a crowd penalty for every stop entered that has a current report.</li>
 *     <li>FEWER_TRANSFERS: as FASTEST + a fixed penalty for every interchange.</li>
 * </ul>
 * All preferences respect suspended connections, closed stations and the step-free requirement.
 * Every weight is non-negative, which keeps Dijkstra's algorithm correct.
 */
@Component
public class RouteCostModel {

    private final MetroPulseProperties.Routing settings;

    public RouteCostModel(MetroPulseProperties properties) {
        this.settings = properties.routing();
    }

    public EdgeCostFunction costFunction(MetroGraph graph, RoutePreference preference, RoutingConditions conditions) {
        return edge -> {
            if (!isUsable(graph, edge, conditions)) {
                return Double.POSITIVE_INFINITY;
            }
            double cost = edge.baseMinutes() + delayFor(graph, edge, conditions);
            if (preference == RoutePreference.LESS_CROWDED) {
                cost += crowdPenalty(conditions.crowdByNode().get(edge.toNodeId()));
            }
            if (preference == RoutePreference.FEWER_TRANSFERS && edge.isTransfer()) {
                cost += settings.transferPenaltyMinutes();
            }
            return cost;
        };
    }

    /** Recomputes the cost components of a path found for the given preference. */
    public CostBreakdown breakdown(MetroGraph graph, PathResult path, RoutePreference preference, RoutingConditions conditions) {
        int ride = 0;
        int transfer = 0;
        int delay = 0;
        int crowd = crowdOnEnteredNodes(path, conditions);
        int transferPenalty = 0;
        int crowded = 0;
        int withData = 0;
        for (GraphEdge edge : path.edges()) {
            if (edge.isTransfer()) {
                transfer += edge.baseMinutes();
                transferPenalty += settings.transferPenaltyMinutes();
            } else {
                ride += edge.baseMinutes();
            }
            delay += delayFor(graph, edge, conditions);
        }
        for (GraphNode node : path.nodes()) {
            CrowdLevel level = conditions.crowdByNode().get(node.id());
            if (level != null) {
                withData++;
                if (level == CrowdLevel.HIGH || level == CrowdLevel.VERY_HIGH) {
                    crowded++;
                }
            }
        }
        double cost = ride + transfer + delay;
        if (preference == RoutePreference.LESS_CROWDED) {
            cost += crowd;
        }
        if (preference == RoutePreference.FEWER_TRANSFERS) {
            cost += transferPenalty;
        }
        return new CostBreakdown(ride, transfer, delay, crowd,
                preference == RoutePreference.FEWER_TRANSFERS ? transferPenalty : 0, crowded, withData, cost);
    }

    public int crowdPenalty(CrowdLevel level) {
        return level == null ? 0 : settings.penaltyFor(level);
    }

    boolean isUsable(MetroGraph graph, GraphEdge edge, RoutingConditions conditions) {
        if (conditions.blockedEdgeIds().contains(edge.id())) {
            return false;
        }
        if (edge.isTransfer()) {
            GraphNode from = graph.node(edge.fromNodeId());
            GraphNode to = graph.node(edge.toNodeId());
            if (!from.stopping() || !to.stopping()) {
                return false; // cannot change trains where trains do not stop
            }
            if (conditions.closedStationIds().contains(from.stationId()) || conditions.closedStationIds().contains(to.stationId())) {
                return false;
            }
            if (conditions.stepFreeRequired()
                    && (conditions.notStepFreeStationIds().contains(from.stationId())
                    || conditions.notStepFreeStationIds().contains(to.stationId()))) {
                return false;
            }
        }
        return true;
    }

    private int delayFor(MetroGraph graph, GraphEdge edge, RoutingConditions conditions) {
        int delay = conditions.edgeDelayMinutes().getOrDefault(edge.id(), 0);
        long toStation = graph.node(edge.toNodeId()).stationId();
        return delay + conditions.stationDelayMinutes().getOrDefault(toStation, 0);
    }

    /** Crowd penalty exactly as Dijkstra saw it: once for every node entered (the origin is not entered). */
    private int crowdOnEnteredNodes(PathResult path, RoutingConditions conditions) {
        int total = 0;
        for (GraphEdge edge : path.edges()) {
            total += crowdPenalty(conditions.crowdByNode().get(edge.toNodeId()));
        }
        return total;
    }
}
