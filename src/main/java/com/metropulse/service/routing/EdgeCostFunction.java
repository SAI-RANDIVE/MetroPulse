package com.metropulse.service.routing;

/**
 * Gives the cost of travelling along one edge. Return {@link Double#POSITIVE_INFINITY} to forbid the edge.
 * Costs must never be negative, otherwise Dijkstra's algorithm would give wrong answers.
 */
@FunctionalInterface
public interface EdgeCostFunction {

    double cost(GraphEdge edge);

    /** Plain estimated minutes - the "fastest" cost with no adjustments. */
    EdgeCostFunction BASE_MINUTES = GraphEdge::baseMinutes;
}
