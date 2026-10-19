package com.metropulse.service.routing;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable weighted directed graph of the metro network, stored as adjacency lists.
 * Build it with {@link MetroGraphBuilder}, which validates every node and edge first.
 */
public final class MetroGraph {

    private final Map<Long, GraphNode> nodes;
    private final Map<Long, List<GraphEdge>> outgoing;
    private final List<String> issues;
    private final int edgeCount;

    MetroGraph(Map<Long, GraphNode> nodes, Map<Long, List<GraphEdge>> outgoing, List<String> issues) {
        this.nodes = Map.copyOf(nodes);
        this.outgoing = Map.copyOf(outgoing);
        this.issues = List.copyOf(issues);
        this.edgeCount = outgoing.values().stream().mapToInt(List::size).sum();
    }

    public boolean contains(long nodeId) {
        return nodes.containsKey(nodeId);
    }

    public GraphNode node(long nodeId) {
        GraphNode node = nodes.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Unknown graph node " + nodeId);
        }
        return node;
    }

    public List<GraphEdge> outgoing(long nodeId) {
        return outgoing.getOrDefault(nodeId, List.of());
    }

    public Collection<GraphNode> nodes() {
        return nodes.values();
    }

    public List<GraphNode> nodesAtStation(long stationId) {
        return nodes.values().stream()
                .filter(n -> n.stationId() == stationId)
                .sorted(Comparator.comparing(GraphNode::lineCode))
                .toList();
    }

    /** The first or last stop of a line, used to describe the direction of travel. */
    public Optional<GraphNode> terminal(long lineId, boolean last) {
        Comparator<GraphNode> bySequence = Comparator.comparingInt(GraphNode::sequenceNo);
        var lineNodes = nodes.values().stream().filter(n -> n.lineId() == lineId);
        return last ? lineNodes.max(bySequence) : lineNodes.min(bySequence);
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return edgeCount;
    }

    /** Data problems found while building (invalid edges are left out of the graph). */
    public List<String> issues() {
        return issues;
    }
}
