package com.metropulse.service.routing;

import com.metropulse.enums.CrowdLevel;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A snapshot of everything that changes route costs at one moment in time.
 *
 * @param crowdByNode           current crowd level per line-station node (only eligible, unexpired reports)
 * @param blockedEdgeIds        edges removed by active SUSPENDED alerts
 * @param edgeDelayMinutes      extra minutes per edge from active DELAY alerts
 * @param stationDelayMinutes   extra minutes for entering any node of a station (station-level DELAY alerts)
 * @param closedStationIds      stations where boarding, alighting and changing are not possible right now
 * @param notStepFreeStationIds stations whose recorded step-free facilities are all out of service
 * @param stepFreeRequired      true when the passenger asked for step-free interchanges
 * @param alerts                the alerts that produced the restrictions, for explanations
 */
public record RoutingConditions(Map<Long, CrowdLevel> crowdByNode,
                                Set<Long> blockedEdgeIds,
                                Map<Long, Integer> edgeDelayMinutes,
                                Map<Long, Integer> stationDelayMinutes,
                                Set<Long> closedStationIds,
                                Set<Long> notStepFreeStationIds,
                                boolean stepFreeRequired,
                                List<AppliedAlert> alerts) {

    public RoutingConditions {
        crowdByNode = Map.copyOf(crowdByNode);
        blockedEdgeIds = Set.copyOf(blockedEdgeIds);
        edgeDelayMinutes = Map.copyOf(edgeDelayMinutes);
        stationDelayMinutes = Map.copyOf(stationDelayMinutes);
        closedStationIds = Set.copyOf(closedStationIds);
        notStepFreeStationIds = Set.copyOf(notStepFreeStationIds);
        alerts = List.copyOf(alerts);
    }

    /** No crowding, alerts or accessibility restrictions. */
    public static RoutingConditions none() {
        return new RoutingConditions(Map.of(), Set.of(), Map.of(), Map.of(), Set.of(), Set.of(), false, List.of());
    }

    /** The same conditions but without alert-based edge blocking (used to explain what an alert changed). */
    public RoutingConditions withoutBlockedEdges() {
        return new RoutingConditions(crowdByNode, Set.of(), edgeDelayMinutes, stationDelayMinutes,
                closedStationIds, notStepFreeStationIds, stepFreeRequired, alerts);
    }

    public RoutingConditions withCrowd(Map<Long, CrowdLevel> crowd) {
        return new RoutingConditions(crowd, blockedEdgeIds, edgeDelayMinutes, stationDelayMinutes,
                closedStationIds, notStepFreeStationIds, stepFreeRequired, alerts);
    }

    public boolean hasCrowdData() {
        return !crowdByNode.isEmpty();
    }

    /** An alert that affects routing, kept short for route explanations. */
    public record AppliedAlert(long id, String title, String impactLabel, Set<Long> edgeIds, Long stationId) {
        public AppliedAlert {
            edgeIds = Set.copyOf(edgeIds);
        }
    }
}
