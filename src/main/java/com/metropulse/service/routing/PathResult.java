package com.metropulse.service.routing;

import java.util.List;

/**
 * An ordered path found by {@link DijkstraRouter}: nodes[0] is the origin, and edges[i] joins nodes[i] to nodes[i+1].
 */
public record PathResult(List<GraphNode> nodes, List<GraphEdge> edges, double totalCost) {

    public PathResult {
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
        if (nodes.size() != edges.size() + 1) {
            throw new IllegalArgumentException("A path needs exactly one more node than edges");
        }
    }

    /** Identity of the path, used to detect duplicate recommendations. */
    public List<Long> edgeIds() {
        return edges.stream().map(GraphEdge::id).toList();
    }

    public long transferCount() {
        return edges.stream().filter(GraphEdge::isTransfer).count();
    }
}
