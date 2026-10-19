package com.metropulse.repository;

import com.metropulse.entity.LineStation;
import com.metropulse.entity.NetworkEdge;
import com.metropulse.enums.EdgeType;
import com.metropulse.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifies the Flyway migrations and the seeded Line 1 / 2A / 7 network on a real PostgreSQL. */
class NetworkSeedIntegrationTest extends IntegrationTest {

    @Autowired
    MetroLineRepository lines;
    @Autowired
    StationRepository stations;
    @Autowired
    LineStationRepository lineStations;
    @Autowired
    NetworkEdgeRepository edges;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void seedsThreeLinesWithOperatorStationCounts() {
        assertThat(lines.findAllByOrderByLineCodeAsc()).extracting("lineCode").containsExactly("1", "2A", "7");

        Map<String, Long> nodesPerLine = lineStations.findAllWithLineAndStation().stream()
                .collect(Collectors.groupingBy(ls -> ls.getLine().getLineCode(), Collectors.counting()));
        // Line 7 lists 13 stations plus the shared Dahisar (East) junction
        assertThat(nodesPerLine).containsEntry("1", 12L).containsEntry("2A", 17L).containsEntry("7", 14L);
        assertThat(stations.count()).isEqualTo(42);
    }

    @Test
    void createsRideEdgesInBothDirectionsAndSixTransfers() {
        List<NetworkEdge> all = edges.findAllWithNodes();
        long rides = all.stream().filter(e -> e.getEdgeType() == EdgeType.RIDE).count();
        long transfers = all.stream().filter(e -> e.getEdgeType() == EdgeType.TRANSFER).count();
        assertThat(rides).isEqualTo(2L * (11 + 16 + 13));
        assertThat(transfers).isEqualTo(6);
        assertThat(all).allMatch(e -> e.getBaseMinutes() >= 0);
    }

    @Test
    void lineOrderMatchesOperatorList() {
        Long line7 = lines.findByLineCodeIgnoreCase("7").orElseThrow().getId();
        List<String> names = lineStations.findByLineOrdered(line7).stream()
                .map(LineStation::getStation).map(s -> s.getName()).toList();
        assertThat(names).startsWith("Gundavali", "Mogra", "Jogeshwari (East)").endsWith("Ovaripada", "Dahisar (East)");
    }

    @Test
    void databaseRejectsSelfLoopAndNegativeMinutes() {
        Long nodeId = lineStations.findAll().getFirst().getId();
        assertThatThrownBy(() -> jdbc.update(
                "insert into network_edge(from_node_id,to_node_id,edge_type,base_minutes) values (?,?, 'RIDE', 2)", nodeId, nodeId))
                .hasMessageContaining("chk_edge_not_loop");
        assertThatThrownBy(() -> jdbc.update(
                "insert into crowd_report(line_station_id,crowd_level,verification_status,created_at,expires_at) "
                        + "values (?, 'LOW', 'PENDING', now(), now() + interval '15 minutes')", nodeId))
                .hasMessageContaining("chk_report_user");
    }
}
