package com.metropulse.service;

import com.metropulse.config.MetroPulseProperties;
import com.metropulse.dto.AlertNotice;
import com.metropulse.dto.StationRef;
import com.metropulse.dto.form.RouteSearchForm;
import com.metropulse.dto.route.RouteOption;
import com.metropulse.dto.route.RoutePlanResult;
import com.metropulse.entity.Station;
import com.metropulse.enums.AccessibilityState;
import com.metropulse.enums.RoutePreference;
import com.metropulse.exception.InvalidRequestException;
import com.metropulse.exception.RouteNotFoundException;
import com.metropulse.service.routing.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates a journey search: load the graph and current conditions, run Dijkstra once per
 * preference with that preference's weights, then describe, de-duplicate and explain the results.
 */
@Service
@Transactional(readOnly = true)
public class RoutePlannerService {

    private final StationService stations;
    private final NetworkGraphService graphs;
    private final RoutingConditionsService conditionsService;
    private final FacilityService facilities;
    private final DijkstraRouter router;
    private final RouteCostModel costModel;
    private final MetroPulseProperties properties;
    private final Clock clock;

    public RoutePlannerService(StationService stations, NetworkGraphService graphs, RoutingConditionsService conditionsService,
                               FacilityService facilities, DijkstraRouter router, RouteCostModel costModel,
                               MetroPulseProperties properties, Clock clock) {
        this.stations = stations;
        this.graphs = graphs;
        this.conditionsService = conditionsService;
        this.facilities = facilities;
        this.router = router;
        this.costModel = costModel;
        this.properties = properties;
        this.clock = clock;
    }

    public RoutePlanResult plan(RouteSearchForm form) {
        Station origin = stations.require(form.getFrom());
        Station destination = stations.require(form.getTo());
        if (origin.getId().equals(destination.getId())) {
            throw new InvalidRequestException("Origin and destination must be different stations");
        }
        RoutePreference requested = form.getPreference() == null ? RoutePreference.FASTEST : form.getPreference();

        MetroGraph graph = graphs.currentGraph();
        RoutingConditions conditions = conditionsService.current(graph, form.isStepFree());
        Set<Long> sources = boardableNodes(graph, origin, conditions);
        Set<Long> targets = boardableNodes(graph, destination, conditions);

        Map<RoutePreference, PathResult> paths = new EnumMap<>(RoutePreference.class);
        for (RoutePreference preference : RoutePreference.values()) {
            router.shortestPath(graph, sources, targets, costModel.costFunction(graph, preference, conditions))
                    .ifPresent(path -> paths.put(preference, path));
        }
        PathResult fastest = paths.get(RoutePreference.FASTEST);
        if (fastest == null) {
            throw new RouteNotFoundException(noRouteMessage(origin, destination, conditions));
        }

        CostBreakdown fastestCost = costModel.breakdown(graph, fastest, RoutePreference.FASTEST, conditions);
        String avoidedAlert = alertAvoidedByFastest(graph, sources, targets, conditions);
        Map<Long, AccessibilityState> accessibility = form.isStepFree() ? facilities.accessibilityByStation() : Map.of();

        List<RoutePreference> order = new ArrayList<>(List.of(requested));
        Arrays.stream(RoutePreference.values()).filter(p -> p != requested).forEach(order::add);

        Map<List<Long>, RoutePreference> seen = new LinkedHashMap<>();
        List<RouteOption> options = new ArrayList<>();
        for (RoutePreference preference : order) {
            PathResult path = paths.get(preference);
            RoutePreference sameAs = seen.putIfAbsent(path.edgeIds(), preference);
            CostBreakdown cost = costModel.breakdown(graph, path, preference, conditions);
            RouteDescriber.Description description = RouteDescriber.describe(graph, path, conditions.crowdByNode());
            boolean sameAsFastest = path.edgeIds().equals(fastest.edgeIds());
            String explanation = RouteExplainer.explain(preference, cost, fastestCost, path.transferCount(),
                    fastest.transferCount(), sameAsFastest, conditions.hasCrowdData(),
                    properties.routing().transferPenaltyMinutes(),
                    sameAsFastest || preference == RoutePreference.FASTEST ? avoidedAlert : null);
            options.add(new RouteOption(preference, preference.getLabel(), preference == requested, sameAs,
                    description.steps(), description.stationCount(),
                    cost.rideMinutes(), cost.transferMinutes(), cost.delayMinutes(), cost.totalMinutes(),
                    (int) path.transferCount(), cost.routeCost(), cost.crowdPenalty(), cost.crowdedStops(),
                    cost.stopsWithCrowdData(), explanation,
                    warnings(origin, destination, cost, form.isStepFree(), accessibility, path)));
        }

        List<AlertNotice> notices = conditions.alerts().stream()
                .map(a -> new AlertNotice(a.id(), a.title(), a.impactLabel())).toList();
        return new RoutePlanResult(new StationRef(origin.getId(), origin.getName()),
                new StationRef(destination.getId(), destination.getName()), requested, form.isStepFree(), options,
                seen.size(), conditions.hasCrowdData(), notices, notes(conditions, seen.size(), form.isStepFree()),
                clock.instant());
    }

    /** Nodes where the passenger can actually board or alight: trains stop and the station is not closed. */
    private static Set<Long> boardableNodes(MetroGraph graph, Station station, RoutingConditions conditions) {
        if (conditions.closedStationIds().contains(station.getId())) {
            throw new InvalidRequestException(station.getName() + " is closed according to a verified service alert");
        }
        Set<Long> nodes = graph.nodesAtStation(station.getId()).stream()
                .filter(GraphNode::stopping).map(GraphNode::id).collect(Collectors.toSet());
        if (nodes.isEmpty()) {
            throw new InvalidRequestException(station.getName() + " has no stopping metro service in the current network data");
        }
        return nodes;
    }

    /** If the best route would use a suspended connection, name the alert so the explanation can mention it. */
    private String alertAvoidedByFastest(MetroGraph graph, Set<Long> sources, Set<Long> targets, RoutingConditions conditions) {
        if (conditions.blockedEdgeIds().isEmpty()) {
            return null;
        }
        RoutingConditions unblocked = conditions.withoutBlockedEdges();
        Optional<PathResult> ideal = router.shortestPath(graph, sources, targets,
                costModel.costFunction(graph, RoutePreference.FASTEST, unblocked));
        if (ideal.isEmpty()) {
            return null;
        }
        Set<Long> used = new HashSet<>(ideal.get().edgeIds());
        return conditions.alerts().stream()
                .filter(a -> a.edgeIds().stream().anyMatch(used::contains) && conditions.blockedEdgeIds().containsAll(a.edgeIds()))
                .map(RoutingConditions.AppliedAlert::title)
                .findFirst().orElse(null);
    }

    private String noRouteMessage(Station origin, Station destination, RoutingConditions conditions) {
        String base = "No route is available between " + origin.getName() + " and " + destination.getName();
        if (!conditions.blockedEdgeIds().isEmpty() || !conditions.closedStationIds().isEmpty()) {
            return base + " right now because of connections suspended by verified service alerts.";
        }
        if (conditions.stepFreeRequired()) {
            return base + " with step-free interchanges. Try again without the step-free option.";
        }
        return base + " in the current network data.";
    }

    private static List<String> warnings(Station origin, Station destination, CostBreakdown cost, boolean stepFree, Map<Long, AccessibilityState> accessibility, PathResult path) {
        List<String> warnings = new ArrayList<>();
        if (cost.crowdedStops() > 0) {
            warnings.add(RouteExplainer.plural(cost.crowdedStops(), "stop") + " on this route currently "
                    + (cost.crowdedStops() == 1 ? "has" : "have") + " HIGH or VERY HIGH crowd reports.");
        }
        if (stepFree) {
            Map<Long, String> keyStations = new LinkedHashMap<>();
            keyStations.put(origin.getId(), origin.getName());
            for (int i = 0; i < path.edges().size(); i++) {
                if (path.edges().get(i).isTransfer()) {
                    GraphNode a = path.nodes().get(i);
                    GraphNode b = path.nodes().get(i + 1);
                    keyStations.put(a.stationId(), a.stationName());
                    keyStations.put(b.stationId(), b.stationName());
                }
            }
            keyStations.put(destination.getId(), destination.getName());
            keyStations.forEach((id, name) -> {
                AccessibilityState state = accessibility.getOrDefault(id, AccessibilityState.UNKNOWN);
                if (state == AccessibilityState.UNKNOWN) {
                    warnings.add("No step-free facility data for " + name + " - check before travelling.");
                } else if (state == AccessibilityState.NOT_STEP_FREE) {
                    warnings.add("Recorded lifts or wheelchair access at " + name + " are out of service.");
                }
            });
        }
        return warnings;
    }

    private List<String> notes(RoutingConditions conditions, int distinctRoutes, boolean stepFree) {
        List<String> notes = new ArrayList<>();
        notes.add("Journey times are prototype estimates (about 2 minutes between stops plus estimated interchange walks), "
                + "not official timetable data.");
        if (!conditions.hasCrowdData()) {
            notes.add("No current crowd reports exist on the network, so crowding is unknown - it is not assumed to be low.");
        }
        if (distinctRoutes == 1) {
            notes.add("Only one distinct route exists for this journey under current conditions.");
        }
        if (stepFree) {
            notes.add("Step-free mode avoids interchanges where recorded lifts or wheelchair access are out of service. "
                    + "Stations without facility data are flagged, never assumed accessible.");
        }
        return notes;
    }
}
