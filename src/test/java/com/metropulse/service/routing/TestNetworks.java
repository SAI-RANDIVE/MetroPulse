package com.metropulse.service.routing;

import com.metropulse.config.MetroPulseProperties;
import com.metropulse.enums.CrowdLevel;
import com.metropulse.enums.EdgeType;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Map;

/**
 * Small hand-made networks for algorithm tests.
 * <pre>
 * Line A (3 min per hop):  S1 - S2 - S3 - S4 - S5 - S6        nodes 1..6, stations 101..106
 * Line B (2 min):               S2 - T                         nodes 11 (S2), 12 (T)
 * Line C (2 min):                    T - S6                    nodes 21 (T), 22 (S6)
 * Line D: an isolated stop Z                                   node 99, station 999
 * Transfers (1 min, both ways): 2<->11, 12<->21, 22<->6
 * </pre>
 * S1 -> S6: the fastest route takes the B/C shortcut (9 min, 2 interchanges);
 * staying on Line A takes 15 min with no interchange.
 */
final class TestNetworks {

    static final long S1 = 101, S2 = 102, S6 = 106, T = 200, Z = 999;

    private TestNetworks() {
    }

    static MetroGraph shortcutNetwork() {
        MetroGraphBuilder b = new MetroGraphBuilder();
        for (int i = 1; i <= 6; i++) {
            b.addNode(node(i, 100 + i, "S" + i, 1, "A", i));
        }
        b.addNode(node(11, S2, "S2", 2, "B", 1));
        b.addNode(node(12, T, "T", 2, "B", 2));
        b.addNode(node(21, T, "T", 3, "C", 1));
        b.addNode(node(22, S6, "S6", 3, "C", 2));
        b.addNode(node(99, Z, "Z", 4, "D", 1));

        long id = 1000;
        for (int i = 1; i < 6; i++) {
            b.addEdge(new GraphEdge(id++, i, i + 1, EdgeType.RIDE, 3, null));
            b.addEdge(new GraphEdge(id++, i + 1, i, EdgeType.RIDE, 3, null));
        }
        b.addEdge(new GraphEdge(2001, 11, 12, EdgeType.RIDE, 2, null));
        b.addEdge(new GraphEdge(2002, 12, 11, EdgeType.RIDE, 2, null));
        b.addEdge(new GraphEdge(3001, 21, 22, EdgeType.RIDE, 2, null));
        b.addEdge(new GraphEdge(3002, 22, 21, EdgeType.RIDE, 2, null));
        transfer(b, 4001, 2, 11);
        transfer(b, 4003, 12, 21);
        transfer(b, 4005, 22, 6);
        return b.build();
    }

    static GraphNode node(long id, long stationId, String name, long lineId, String lineCode, int seq) {
        return new GraphNode(id, stationId, name, lineId, lineCode, "Line " + lineCode, "#123456", "#FFFFFF", seq, true);
    }

    private static void transfer(MetroGraphBuilder b, long id, long x, long y) {
        b.addEdge(new GraphEdge(id, x, y, EdgeType.TRANSFER, 1, "walk"));
        b.addEdge(new GraphEdge(id + 1, y, x, EdgeType.TRANSFER, 1, "walk"));
    }

    static MetroPulseProperties properties() {
        return new MetroPulseProperties("test", ZoneId.of("Asia/Kolkata"),
                new MetroPulseProperties.Security(null, null, "Admin"),
                new MetroPulseProperties.Demo(false),
                new MetroPulseProperties.Crowd(Duration.ofMinutes(15), Duration.ofHours(6), Duration.ofMinutes(5), 6),
                new MetroPulseProperties.Routing(Map.of(CrowdLevel.LOW, 0, CrowdLevel.MODERATE, 2,
                        CrowdLevel.HIGH, 6, CrowdLevel.VERY_HIGH, 12), 20));
    }
}
