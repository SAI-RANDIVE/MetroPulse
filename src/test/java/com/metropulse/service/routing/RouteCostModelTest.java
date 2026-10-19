package com.metropulse.service.routing;

import com.metropulse.enums.CrowdLevel;
import com.metropulse.enums.EdgeType;
import com.metropulse.enums.RoutePreference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.metropulse.service.routing.TestNetworks.node;
import static org.assertj.core.api.Assertions.assertThat;

class RouteCostModelTest {

    private final RouteCostModel model = new RouteCostModel(TestNetworks.properties());
    private final MetroGraph graph = TestNetworks.shortcutNetwork();
    private final GraphEdge transfer = new GraphEdge(4001, 2, 11, EdgeType.TRANSFER, 1, null);
    private final GraphEdge ride = new GraphEdge(2001, 11, 12, EdgeType.RIDE, 2, null);

    @Test
    void transferPenaltyOnlyForFewerTransfersPreference() {
        assertThat(model.costFunction(graph, RoutePreference.FASTEST, RoutingConditions.none()).cost(transfer)).isEqualTo(1);
        assertThat(model.costFunction(graph, RoutePreference.FEWER_TRANSFERS, RoutingConditions.none()).cost(transfer)).isEqualTo(21);
        assertThat(model.costFunction(graph, RoutePreference.FEWER_TRANSFERS, RoutingConditions.none()).cost(ride)).isEqualTo(2);
    }

    @Test
    void crowdPenaltyOnlyForLessCrowdedPreference() {
        RoutingConditions crowd = RoutingConditions.none().withCrowd(Map.of(12L, CrowdLevel.HIGH));
        assertThat(model.costFunction(graph, RoutePreference.FASTEST, crowd).cost(ride)).isEqualTo(2);
        assertThat(model.costFunction(graph, RoutePreference.LESS_CROWDED, crowd).cost(ride)).isEqualTo(8);
    }

    @Test
    void noCrowdDataMeansNoPenaltyNotLowCrowding() {
        assertThat(model.costFunction(graph, RoutePreference.LESS_CROWDED, RoutingConditions.none()).cost(ride)).isEqualTo(2);
        assertThat(model.crowdPenalty(null)).isZero();
    }

    @Test
    void closedStationForbidsTransfersButNotPassingThrough() {
        RoutingConditions closed = new RoutingConditions(Map.of(), Set.of(), Map.of(), Map.of(), Set.of(TestNetworks.S2), Set.of(), false, List.of());
        var cost = model.costFunction(graph, RoutePreference.FASTEST, closed);
        assertThat(cost.cost(transfer)).isInfinite();
        assertThat(cost.cost(new GraphEdge(1002, 2, 3, EdgeType.RIDE, 3, null))).isEqualTo(3);
    }

    @Test
    void nonStoppingNodeCannotBeUsedForTransfer() {
        MetroGraph g = new MetroGraphBuilder()
                .addNode(new GraphNode(1, 1, "P", 1, "A", "A", "#000000", "#FFFFFF", 1, false))
                .addNode(node(2, 1, "P", 2, "B", 1))
                .addEdge(new GraphEdge(5, 1, 2, EdgeType.TRANSFER, 2, null))
                .build();
        assertThat(model.costFunction(g, RoutePreference.FASTEST, RoutingConditions.none()).cost(g.outgoing(1).getFirst())).isInfinite();
    }

    @Test
    void stepFreeRequirementAvoidsInterchangesWithBrokenLifts() {
        RoutingConditions stepFree = new RoutingConditions(Map.of(), Set.of(), Map.of(), Map.of(), Set.of(), Set.of(TestNetworks.S2), true, List.of());
        RoutingConditions ignored = new RoutingConditions(Map.of(), Set.of(), Map.of(), Map.of(), Set.of(), Set.of(TestNetworks.S2), false, List.of());
        assertThat(model.costFunction(graph, RoutePreference.FASTEST, stepFree).cost(transfer)).isInfinite();
        assertThat(model.costFunction(graph, RoutePreference.FASTEST, ignored).cost(transfer)).isEqualTo(1);
    }

    @Test
    void breakdownSeparatesRideTransferDelayAndPenalties() {
        RoutingConditions conditions = new RoutingConditions(Map.of(12L, CrowdLevel.VERY_HIGH), Set.of(), Map.of(2001L, 5),
                Map.of(), Set.of(), Set.of(), false, List.of());
        PathResult path = new DijkstraRouter().shortestPath(graph, Set.of(1L), Set.of(22L),
                model.costFunction(graph, RoutePreference.FASTEST, conditions)).orElseThrow();
        // 1-2 (3) + transfer (1) + 11-12 (2 + 5 delay) + transfer (1) + 21-22 (2) = 14 min, still under 15 on Line A
        CostBreakdown b = model.breakdown(graph, path, RoutePreference.LESS_CROWDED, conditions);
        assertThat(b.rideMinutes()).isEqualTo(7);
        assertThat(b.transferMinutes()).isEqualTo(2);
        assertThat(b.delayMinutes()).isEqualTo(5);
        assertThat(b.totalMinutes()).isEqualTo(14);
        assertThat(b.crowdPenalty()).isEqualTo(12);
        assertThat(b.crowdedStops()).isEqualTo(1);
        assertThat(b.routeCost()).isEqualTo(26.0);
    }
}
