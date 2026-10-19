package com.metropulse.service.routing;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Dijkstra's shortest-path algorithm over a {@link MetroGraph}.
 * <p>
 * Several start nodes are allowed because one station can sit on several lines
 * (e.g. Dahisar (East) on Line 2A and Line 7): all of them start with cost 0.
 * The search stops as soon as any target node is taken from the priority queue,
 * which is the cheapest way to reach the destination station.
 * <p>
 * Complexity: O((V + E) log V) with a binary-heap priority queue.
 */
@Component
public class DijkstraRouter {

    /** Queue entry: a node and the cost with which it was reached. */
    private record QueueEntry(long nodeId, double cost) {
    }

    public Optional<PathResult> shortestPath(MetroGraph graph, Set<Long> sources, Set<Long> targets,
                                             EdgeCostFunction costFunction) {
        requireKnownNodes(graph, sources, "source");
        requireKnownNodes(graph, targets, "target");

        Map<Long, Double> best = new HashMap<>();
        Map<Long, GraphEdge> arrivedBy = new HashMap<>();
        Set<Long> settled = new HashSet<>();
        PriorityQueue<QueueEntry> queue = new PriorityQueue<>((a, b) -> Double.compare(a.cost(), b.cost()));

        for (long source : sources) {
            best.put(source, 0.0);
            queue.add(new QueueEntry(source, 0.0));
        }

        while (!queue.isEmpty()) {
            QueueEntry current = queue.poll();
            if (!settled.add(current.nodeId())) {
                continue; // an older, more expensive entry for a node we already settled
            }
            if (targets.contains(current.nodeId())) {
                return Optional.of(rebuildPath(graph, current.nodeId(), arrivedBy, current.cost()));
            }
            for (GraphEdge edge : graph.outgoing(current.nodeId())) {
                double edgeCost = costFunction.cost(edge);
                if (Double.isNaN(edgeCost) || edgeCost < 0) {
                    throw new IllegalStateException("Edge " + edge.id() + " has an invalid cost " + edgeCost);
                }
                if (Double.isInfinite(edgeCost) || settled.contains(edge.toNodeId())) {
                    continue; // forbidden edge, or neighbour already final
                }
                double newCost = current.cost() + edgeCost;
                if (newCost < best.getOrDefault(edge.toNodeId(), Double.POSITIVE_INFINITY)) {
                    best.put(edge.toNodeId(), newCost);
                    arrivedBy.put(edge.toNodeId(), edge);
                    queue.add(new QueueEntry(edge.toNodeId(), newCost));
                }
            }
        }
        return Optional.empty(); // destination unreachable
    }

    /** Walks back from the target along the remembered edges, then reverses into origin-to-destination order. */
    private PathResult rebuildPath(MetroGraph graph, long target, Map<Long, GraphEdge> arrivedBy, double cost) {
        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();
        long nodeId = target;
        nodes.add(graph.node(nodeId));
        while (arrivedBy.containsKey(nodeId)) {
            GraphEdge edge = arrivedBy.get(nodeId);
            edges.add(edge);
            nodeId = edge.fromNodeId();
            nodes.add(graph.node(nodeId));
        }
        Collections.reverse(nodes);
        Collections.reverse(edges);
        return new PathResult(nodes, edges, cost);
    }

    private static void requireKnownNodes(MetroGraph graph, Set<Long> ids, String role) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("At least one " + role + " node is required");
        }
        for (long id : ids) {
            if (!graph.contains(id)) {
                throw new IllegalArgumentException("Unknown " + role + " node " + id);
            }
        }
    }
}
