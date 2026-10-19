package com.metropulse.service.routing;

import com.metropulse.enums.CrowdLevel;
import com.metropulse.enums.RoutePreference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.metropulse.service.routing.TestNetworks.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DijkstraRouterTest {

    private final DijkstraRouter router = new DijkstraRouter();
    private final RouteCostModel costModel = new RouteCostModel(TestNetworks.properties());
    private final MetroGraph graph = shortcutNetwork();

    private PathResult route(long fromStation, long toStation, RoutePreference preference, RoutingConditions conditions) {
        return router.shortestPath(graph, ids(fromStation), ids(toStation), costModel.costFunction(graph, preference, conditions))
                .orElseThrow();
    }

    private Set<Long> ids(long stationId) {
        return Set.copyOf(graph.nodesAtStation(stationId).stream().map(GraphNode::id).toList());
    }

    private static List<Long> nodeIds(PathResult path) {
        return path.nodes().stream().map(GraphNode::id).toList();
    }

    @Test
    void sameLineRouteFollowsStopsInOrder() {
        PathResult path = route(S1, 104, RoutePreference.FASTEST, RoutingConditions.none());
        assertThat(nodeIds(path)).containsExactly(1L, 2L, 3L, 4L);
        assertThat(path.totalCost()).isEqualTo(9.0);
        assertThat(path.transferCount()).isZero();
    }

    @Test
    void fastestRouteUsesInterchangesAndIsRebuiltInOrder() {
        PathResult path = route(S1, S6, RoutePreference.FASTEST, RoutingConditions.none());
        assertThat(nodeIds(path)).containsExactly(1L, 2L, 11L, 12L, 21L, 22L);
        assertThat(path.nodes().getFirst().stationId()).isEqualTo(S1);
        assertThat(path.nodes().getLast().stationId()).isEqualTo(S6);
        assertThat(path.totalCost()).isEqualTo(3 + 1 + 2 + 1 + 2);
        assertThat(path.transferCount()).isEqualTo(2);
        // every edge joins consecutive path nodes
        for (int i = 0; i < path.edges().size(); i++) {
            assertThat(path.edges().get(i).fromNodeId()).isEqualTo(path.nodes().get(i).id());
            assertThat(path.edges().get(i).toNodeId()).isEqualTo(path.nodes().get(i + 1).id());
        }
    }

    @Test
    void fewerTransfersPreferenceChoosesDifferentRouteThanFastest() {
        PathResult fastest = route(S1, S6, RoutePreference.FASTEST, RoutingConditions.none());
        PathResult fewer = route(S1, S6, RoutePreference.FEWER_TRANSFERS, RoutingConditions.none());
        assertThat(fewer.transferCount()).isZero();
        assertThat(nodeIds(fewer)).containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
        assertThat(fewer.edgeIds()).isNotEqualTo(fastest.edgeIds());
        // 15 min on Line A beats 9 min + 2 x 20 min transfer penalty
        assertThat(fewer.totalCost()).isEqualTo(15.0);
    }

    @Test
    void crowdPenaltyMovesLessCrowdedRouteAwayFromCrowdedStop() {
        RoutingConditions crowded = RoutingConditions.none().withCrowd(Map.of(12L, CrowdLevel.VERY_HIGH));
        PathResult fastest = route(S1, S6, RoutePreference.FASTEST, crowded);
        PathResult lessCrowded = route(S1, S6, RoutePreference.LESS_CROWDED, crowded);
        assertThat(nodeIds(fastest)).contains(12L);          // fastest ignores crowding
        assertThat(nodeIds(lessCrowded)).doesNotContain(12L); // 9 + 12 penalty > 15
        assertThat(lessCrowded.totalCost()).isEqualTo(15.0);
    }

    @Test
    void smallCrowdPenaltyDoesNotOutweighLargeTimeSaving() {
        RoutingConditions moderate = RoutingConditions.none().withCrowd(Map.of(12L, CrowdLevel.MODERATE));
        PathResult lessCrowded = route(S1, S6, RoutePreference.LESS_CROWDED, moderate);
        assertThat(nodeIds(lessCrowded)).contains(12L);       // 9 + 2 = 11 < 15
        assertThat(lessCrowded.totalCost()).isEqualTo(11.0);
    }

    @Test
    void blockedConnectionIsAvoided() {
        RoutingConditions blocked = new RoutingConditions(Map.of(), Set.of(2001L), Map.of(), Map.of(), Set.of(), Set.of(), false, List.of());
        PathResult path = route(S1, S6, RoutePreference.FASTEST, blocked);
        assertThat(path.edgeIds()).doesNotContain(2001L);
        assertThat(nodeIds(path)).containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
    }

    @Test
    void delayOnConnectionIsAddedToCost() {
        RoutingConditions delayed = new RoutingConditions(Map.of(), Set.of(), Map.of(2001L, 10), Map.of(), Set.of(), Set.of(), false, List.of());
        PathResult path = route(S1, S6, RoutePreference.FASTEST, delayed);
        assertThat(nodeIds(path)).containsExactly(1L, 2L, 3L, 4L, 5L, 6L); // 9 + 10 = 19 > 15
    }

    @Test
    void disconnectedNodesHaveNoRoute() {
        // (inactive database edges are dropped by MetroGraphBuilder.fromEntities - see RoutePlannerIntegrationTest)
        MetroGraph disconnected = new MetroGraphBuilder()
                .addNode(node(1, 1, "P", 1, "A", 1)).addNode(node(2, 2, "Q", 1, "A", 2)).build();
        assertThat(router.shortestPath(disconnected, Set.of(1L), Set.of(2L), EdgeCostFunction.BASE_MINUTES)).isEmpty();
    }

    @Test
    void unreachableDestinationGivesEmptyResult() {
        assertThat(router.shortestPath(graph, ids(S1), ids(Z), EdgeCostFunction.BASE_MINUTES)).isEmpty();
    }

    @Test
    void unknownNodesAreRejected() {
        assertThatThrownBy(() -> router.shortestPath(graph, Set.of(12345L), ids(S6), EdgeCostFunction.BASE_MINUTES))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("12345");
        assertThatThrownBy(() -> router.shortestPath(graph, Set.of(), ids(S6), EdgeCostFunction.BASE_MINUTES))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeCostIsRefused() {
        assertThatThrownBy(() -> router.shortestPath(graph, ids(S1), ids(S6), edge -> -1))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void stationOnTwoLinesCanStartFromEitherLine() {
        // S2 is on Line A (node 2) and Line B (node 11): T is reached directly on Line B without a transfer
        PathResult path = route(S2, T, RoutePreference.FASTEST, RoutingConditions.none());
        assertThat(nodeIds(path)).containsExactly(11L, 12L);
        assertThat(path.transferCount()).isZero();
    }
}
