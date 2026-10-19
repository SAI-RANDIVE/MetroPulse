package com.metropulse.service;

import com.metropulse.entity.NetworkEdge;
import com.metropulse.entity.ServiceAlert;
import com.metropulse.enums.AlertImpact;
import com.metropulse.enums.EdgeType;
import com.metropulse.service.routing.GraphEdge;
import com.metropulse.service.routing.GraphNode;
import com.metropulse.service.routing.MetroGraph;
import com.metropulse.service.routing.RoutingConditions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Collects current crowding, in-effect alerts and accessibility data into one {@link RoutingConditions}.
 * <p>
 * How an in-effect alert is applied (INFORMATION alerts never change routes):
 * <ul>
 *     <li>with affected connections: SUSPENDED removes them, DELAY adds its minutes to each;</li>
 *     <li>station-level (no connections): SUSPENDED closes the station for boarding, alighting and changing,
 *         DELAY adds its minutes to every arrival at the station;</li>
 *     <li>line-wide (no station, no connections): SUSPENDED removes every ride connection of that line.</li>
 * </ul>
 * Only connections named by the alert are affected; unrelated station data is never modified.
 */
@Service
@Transactional(readOnly = true)
public class RoutingConditionsService {

    private final CrowdStatusService crowdStatus;
    private final ServiceAlertService alerts;
    private final FacilityService facilities;

    public RoutingConditionsService(CrowdStatusService crowdStatus, ServiceAlertService alerts, FacilityService facilities) {
        this.crowdStatus = crowdStatus;
        this.alerts = alerts;
        this.facilities = facilities;
    }

    public RoutingConditions current(MetroGraph graph, boolean stepFreeRequired) {
        Set<Long> blocked = new HashSet<>();
        Map<Long, Integer> edgeDelays = new HashMap<>();
        Map<Long, Integer> stationDelays = new HashMap<>();
        Set<Long> closedStations = new HashSet<>();
        List<RoutingConditions.AppliedAlert> applied = new ArrayList<>();

        for (ServiceAlert alert : alerts.inEffect()) {
            if (alert.getImpact() == AlertImpact.INFORMATION) {
                continue;
            }
            Set<Long> edgeIds = alert.getAffectedEdges().stream().map(NetworkEdge::getId).collect(Collectors.toSet());
            Long stationId = null;
            if (edgeIds.isEmpty() && alert.getStation() != null) {
                stationId = alert.getStation().getId();
            } else if (edgeIds.isEmpty() && alert.getLine() != null && alert.getImpact() == AlertImpact.SUSPENDED) {
                edgeIds = rideEdgesOfLine(graph, alert.getLine().getId());
            }

            if (alert.getImpact() == AlertImpact.SUSPENDED) {
                blocked.addAll(edgeIds);
                if (stationId != null) {
                    closedStations.add(stationId);
                }
            } else { // DELAY
                for (Long edgeId : edgeIds) {
                    edgeDelays.merge(edgeId, alert.getDelayMinutes(), Integer::sum);
                }
                if (stationId != null) {
                    stationDelays.merge(stationId, alert.getDelayMinutes(), Integer::sum);
                }
            }
            if (!edgeIds.isEmpty() || stationId != null) {
                applied.add(new RoutingConditions.AppliedAlert(alert.getId(), alert.getTitle(),
                        alert.getImpact().getLabel(), edgeIds, stationId));
            }
        }

        Set<Long> notStepFree = stepFreeRequired ? facilities.notStepFreeStationIds() : Set.of();
        return new RoutingConditions(crowdStatus.currentLevels(), blocked, edgeDelays, stationDelays,
                closedStations, notStepFree, stepFreeRequired, applied);
    }

    private static Set<Long> rideEdgesOfLine(MetroGraph graph, long lineId) {
        Set<Long> ids = new HashSet<>();
        for (GraphNode node : graph.nodes()) {
            if (node.lineId() != lineId) {
                continue;
            }
            for (GraphEdge edge : graph.outgoing(node.id())) {
                if (edge.type() == EdgeType.RIDE) {
                    ids.add(edge.id());
                }
            }
        }
        return ids;
    }
}
