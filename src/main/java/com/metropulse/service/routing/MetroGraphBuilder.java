package com.metropulse.service.routing;

import com.metropulse.entity.LineStation;
import com.metropulse.entity.NetworkEdge;
import com.metropulse.enums.EdgeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a {@link MetroGraph} and refuses to let inconsistent data into it.
 * <ul>
 *     <li>edges pointing at unknown nodes are skipped,</li>
 *     <li>a RIDE edge must join consecutive stops of the same line,</li>
 *     <li>a TRANSFER edge must join nodes of two different lines,</li>
 *     <li>negative travel times are rejected (Dijkstra requires non-negative weights),</li>
 *     <li>inactive edges are simply not added.</li>
 * </ul>
 * Every skipped item is recorded in {@link MetroGraph#issues()} so administrators can see it.
 */
public final class MetroGraphBuilder {

    private final Map<Long, GraphNode> nodes = new LinkedHashMap<>();
    private final List<GraphEdge> edges = new ArrayList<>();
    private final List<String> issues = new ArrayList<>();

    public MetroGraphBuilder addNode(GraphNode node) {
        if (nodes.putIfAbsent(node.id(), node) != null) {
            issues.add("Duplicate node " + node.id() + " ignored");
        }
        return this;
    }

    public MetroGraphBuilder addEdge(GraphEdge edge) {
        edges.add(edge);
        return this;
    }

    public MetroGraph build() {
        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        for (GraphEdge edge : edges) {
            String problem = validate(edge);
            if (problem != null) {
                issues.add("Edge " + edge.id() + " skipped: " + problem);
                continue;
            }
            outgoing.computeIfAbsent(edge.fromNodeId(), k -> new ArrayList<>()).add(edge);
        }
        return new MetroGraph(nodes, outgoing, issues);
    }

    private String validate(GraphEdge edge) {
        GraphNode from = nodes.get(edge.fromNodeId());
        GraphNode to = nodes.get(edge.toNodeId());
        if (from == null || to == null) {
            return "refers to a node that is not part of the network";
        }
        if (edge.baseMinutes() < 0) {
            return "negative travel time";
        }
        if (edge.type() == EdgeType.RIDE) {
            if (from.lineId() != to.lineId()) {
                return "ride edge joins two different lines";
            }
            if (Math.abs(from.sequenceNo() - to.sequenceNo()) != 1) {
                return "ride edge joins stops that are not consecutive";
            }
        } else if (from.lineId() == to.lineId()) {
            return "transfer edge must join two different lines";
        }
        return null;
    }

    /** Converts database rows into a graph. Station must be OPEN and node operational for trains to stop. */
    public static MetroGraph fromEntities(List<LineStation> lineStations, List<NetworkEdge> networkEdges) {
        MetroGraphBuilder builder = new MetroGraphBuilder();
        for (LineStation ls : lineStations) {
            var line = ls.getLine();
            var station = ls.getStation();
            builder.addNode(new GraphNode(ls.getId(), station.getId(), station.getName(),
                    line.getId(), line.getLineCode(), line.getName(), line.getColor(), line.getTextColor(),
                    ls.getSequenceNo(), ls.isOperational() && station.isOpen()));
        }
        for (NetworkEdge e : networkEdges) {
            if (e.isActive()) {
                builder.addEdge(new GraphEdge(e.getId(), e.getFromNode().getId(), e.getToNode().getId(),
                        e.getEdgeType(), e.getBaseMinutes(), e.getNote()));
            }
        }
        return builder.build();
    }
}
