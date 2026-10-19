package com.metropulse.service.routing;

import com.metropulse.enums.EdgeType;
import org.junit.jupiter.api.Test;

import static com.metropulse.service.routing.TestNetworks.node;
import static org.assertj.core.api.Assertions.assertThat;

class MetroGraphBuilderTest {

    @Test
    void invalidEdgesAreExcludedAndReported() {
        MetroGraph graph = new MetroGraphBuilder()
                .addNode(node(1, 1, "P", 1, "A", 1))
                .addNode(node(2, 2, "Q", 1, "A", 2))
                .addNode(node(3, 3, "R", 1, "A", 3))
                .addNode(node(4, 4, "S", 2, "B", 1))
                .addEdge(new GraphEdge(10, 1, 2, EdgeType.RIDE, 2, null))      // valid
                .addEdge(new GraphEdge(11, 1, 3, EdgeType.RIDE, 2, null))      // skips a stop
                .addEdge(new GraphEdge(12, 3, 4, EdgeType.RIDE, 2, null))      // ride across lines
                .addEdge(new GraphEdge(13, 1, 2, EdgeType.TRANSFER, 2, null))  // transfer within one line
                .addEdge(new GraphEdge(14, 2, 77, EdgeType.RIDE, 2, null))     // unknown node
                .addEdge(new GraphEdge(15, 2, 3, EdgeType.RIDE, -1, null))     // negative time
                .addEdge(new GraphEdge(16, 3, 4, EdgeType.TRANSFER, 3, null))  // valid
                .build();

        assertThat(graph.edgeCount()).isEqualTo(2);
        assertThat(graph.outgoing(1)).extracting(GraphEdge::id).containsExactly(10L);
        assertThat(graph.issues()).hasSize(5)
                .anyMatch(s -> s.contains("not consecutive"))
                .anyMatch(s -> s.contains("different lines"))
                .anyMatch(s -> s.contains("negative"));
    }

    @Test
    void terminalsDescribeDirection() {
        MetroGraph graph = TestNetworks.shortcutNetwork();
        assertThat(graph.terminal(1, true)).map(GraphNode::stationName).contains("S6");
        assertThat(graph.terminal(1, false)).map(GraphNode::stationName).contains("S1");
    }
}
