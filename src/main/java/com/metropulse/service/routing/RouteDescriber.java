package com.metropulse.service.routing;

import com.metropulse.dto.route.RouteLeg;
import com.metropulse.dto.route.RouteStep;
import com.metropulse.dto.route.RouteStop;
import com.metropulse.dto.route.RouteTransfer;
import com.metropulse.enums.CrowdLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns a raw node/edge path into passenger language: rides on a line ("legs") separated by transfers.
 */
public final class RouteDescriber {

    public record Description(List<RouteStep> steps, int stationCount) {
    }

    private RouteDescriber() {
    }

    public static Description describe(MetroGraph graph, PathResult path, Map<Long, CrowdLevel> crowd) {
        List<RouteStep> steps = new ArrayList<>();
        List<GraphNode> legNodes = new ArrayList<>(List.of(path.nodes().getFirst()));
        int legMinutes = 0;

        for (int i = 0; i < path.edges().size(); i++) {
            GraphEdge edge = path.edges().get(i);
            GraphNode next = path.nodes().get(i + 1);
            if (edge.isTransfer()) {
                closeLeg(graph, legNodes, legMinutes, crowd, steps);
                GraphNode from = path.nodes().get(i);
                steps.add(RouteStep.of(new RouteTransfer(from.stationName(), from.lineCode(), next.stationName(), next.lineCode(),
                        edge.baseMinutes(), edge.note())));
                legNodes = new ArrayList<>(List.of(next));
                legMinutes = 0;
            } else {
                legNodes.add(next);
                legMinutes += edge.baseMinutes();
            }
        }
        closeLeg(graph, legNodes, legMinutes, crowd, steps);

        int stations = (int) path.nodes().stream().map(GraphNode::stationId).distinct().count();
        return new Description(steps, stations);
    }

    private static void closeLeg(MetroGraph graph, List<GraphNode> nodes, int minutes,
                                 Map<Long, CrowdLevel> crowd, List<RouteStep> steps) {
        if (nodes.size() < 2) {
            return; // no ride happened (e.g. the journey starts with a walk to another station)
        }
        GraphNode first = nodes.getFirst();
        GraphNode last = nodes.getLast();
        boolean forward = last.sequenceNo() > first.sequenceNo();
        String towards = graph.terminal(first.lineId(), forward).map(GraphNode::stationName).orElse(last.stationName());
        List<RouteStop> stops = nodes.stream()
                .map(n -> new RouteStop(n.stationId(), n.stationName(), n.id(), crowd.get(n.id())))
                .toList();
        steps.add(RouteStep.of(new RouteLeg(first.lineCode(), first.lineName(), first.lineColor(), first.lineTextColor(),
                towards, stops, minutes)));
    }
}
