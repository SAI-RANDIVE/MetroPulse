package com.metropulse.web;

import com.metropulse.entity.CrowdReport;
import com.metropulse.entity.LineStation;
import com.metropulse.enums.CrowdLevel;
import com.metropulse.repository.CrowdReportRepository;
import com.metropulse.repository.NetworkEdgeRepository;
import com.metropulse.service.LineService;
import com.metropulse.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** GET /api/routes on the real seeded Line 1 / 2A / 7 network. */
@Transactional
class RouteApiIntegrationTest extends IntegrationTest {

    @Autowired
    CrowdReportRepository reports;
    @Autowired
    NetworkEdgeRepository edges;
    @Autowired
    LineService lineService;

    private void report(String line, String station, CrowdLevel level) {
        LineStation node = lineStationRepository.findById(nodeId(line, station)).orElseThrow();
        Instant now = Instant.now();
        reports.save(new CrowdReport(node, commuter("reporter@test.local"), level, null, now, now.plus(Duration.ofMinutes(15))));
    }

    @Test
    void sameLineRouteListsStopsInOrderWithoutInterchange() throws Exception {
        mvc.perform(get("/api/routes").param("from", "" + stationId("VERSOVA")).param("to", "" + stationId("GHATKOPAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].preference").value("FASTEST"))
                .andExpect(jsonPath("$.options[0].interchanges").value(0))
                .andExpect(jsonPath("$.options[0].totalMinutes").value(22))
                .andExpect(jsonPath("$.options[0].steps", hasSize(1)))
                .andExpect(jsonPath("$.options[0].steps[0].ride.stops", hasSize(12)))
                .andExpect(jsonPath("$.options[0].steps[0].ride.stops[0].stationName").value("Versova"))
                .andExpect(jsonPath("$.options[0].steps[0].ride.stops[1].stationName").value("D N Nagar"))
                .andExpect(jsonPath("$.options[0].steps[0].ride.stops[11].stationName").value("Ghatkopar"))
                .andExpect(jsonPath("$.options[0].steps[0].ride.towards").value("Ghatkopar"))
                .andExpect(jsonPath("$.distinctRouteCount").value(1));
    }

    @Test
    void interchangeRouteUsesWalkingTransfers() throws Exception {
        mvc.perform(get("/api/routes").param("from", "" + stationId("ANDHERI_WEST")).param("to", "" + stationId("GUNDAVALI")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].interchanges").value(2))
                .andExpect(jsonPath("$.options[0].steps[0].transfer.toStation").value("D N Nagar"))
                .andExpect(jsonPath("$.options[0].steps[1].ride.lineCode").value("1"))
                .andExpect(jsonPath("$.options[0].steps[2].transfer.toStation").value("Gundavali"))
                .andExpect(jsonPath("$.options[0].totalMinutes").value(22));
    }

    @Test
    void withoutCrowdDataLessCrowdedIsHonestlyTheSameRoute() throws Exception {
        mvc.perform(get("/api/routes").param("from", "" + stationId("GOREGAON_WEST")).param("to", "" + stationId("GOREGAON_EAST"))
                        .param("preference", "LESS_CROWDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.crowdDataAvailable").value(false))
                .andExpect(jsonPath("$.options[0].preference").value("LESS_CROWDED"))
                .andExpect(jsonPath("$.options[0].recommended").value(true))
                .andExpect(jsonPath("$.options[0].explanation", containsString("crowding is unknown")))
                .andExpect(jsonPath("$.notes", hasItem(containsString("not assumed to be low"))));
    }

    @Test
    void fastestAndFewerTransfersDifferOnTheLoop() throws Exception {
        // Goregaon (W) -> Goregaon (E): south via Line 1 is faster (2 changes); north via Dahisar (E) has 1 change
        mvc.perform(get("/api/routes").param("from", "" + stationId("GOREGAON_WEST")).param("to", "" + stationId("GOREGAON_EAST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].preference").value("FASTEST"))
                .andExpect(jsonPath("$.options[0].totalMinutes").value(34))
                .andExpect(jsonPath("$.options[0].interchanges").value(2))
                .andExpect(jsonPath("$.options[2].preference").value("FEWER_TRANSFERS"))
                .andExpect(jsonPath("$.options[2].interchanges").value(1))
                .andExpect(jsonPath("$.options[2].totalMinutes").value(50))
                .andExpect(jsonPath("$.options[2].sameRouteAs").value(nullValue()))
                .andExpect(jsonPath("$.distinctRouteCount").value(2));
    }

    @Test
    void crowdReportsMoveTheLessCrowdedRoute() throws Exception {
        report("1", "ANDHERI", CrowdLevel.VERY_HIGH);
        report("1", "WEH", CrowdLevel.HIGH);
        report("2A", "ANDHERI_WEST", CrowdLevel.MODERATE);
        mvc.perform(get("/api/routes").param("from", "" + stationId("GOREGAON_WEST")).param("to", "" + stationId("GOREGAON_EAST"))
                        .param("preference", "LESS_CROWDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.crowdDataAvailable").value(true))
                .andExpect(jsonPath("$.options[0].preference").value("LESS_CROWDED"))
                .andExpect(jsonPath("$.options[0].interchanges").value(1))
                .andExpect(jsonPath("$.options[0].crowdPenalty").value(0))
                .andExpect(jsonPath("$.options[0].explanation", containsString("fewer reported crowding issues")))
                .andExpect(jsonPath("$.options[1].preference").value("FASTEST"))
                .andExpect(jsonPath("$.options[1].crowdPenalty").value(20));
    }

    @Test
    void expiredReportsDoNotAffectRouting() throws Exception {
        LineStation andheri = lineStationRepository.findById(nodeId("1", "ANDHERI")).orElseThrow();
        Instant past = Instant.now().minus(Duration.ofHours(1));
        reports.save(new CrowdReport(andheri, commuter("old@test.local"), CrowdLevel.VERY_HIGH, null, past, past.plus(Duration.ofMinutes(15))));
        mvc.perform(get("/api/routes").param("from", "" + stationId("GOREGAON_WEST")).param("to", "" + stationId("GOREGAON_EAST")))
                .andExpect(jsonPath("$.crowdDataAvailable").value(false));
    }

    @Test
    void inactiveConnectionIsLeftOutOfRouting() throws Exception {
        // switch off both directions between Azad Nagar and Andheri on Line 1
        long a = nodeId("1", "AZAD_NAGAR");
        long b = nodeId("1", "ANDHERI");
        for (long[] pair : new long[][]{{a, b}, {b, a}}) {
            long edge = edges.findByFromNodeIdAndToNodeIdAndEdgeType(pair[0], pair[1], com.metropulse.enums.EdgeType.RIDE).orElseThrow().getId();
            lineService.updateEdge(edge, 2, false, admin());
        }
        mvc.perform(get("/api/routes").param("from", "" + stationId("VERSOVA")).param("to", "" + stationId("GHATKOPAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].interchanges").value(greaterThan(0)))
                .andExpect(jsonPath("$.options[0].steps[*].ride.lineCode", hasItems("2A", "7")));
    }

    @Test
    void missingParametersGiveFieldErrors() throws Exception {
        mvc.perform(get("/api/routes"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItems("from", "to")));
    }

    @Test
    void sameOriginAndDestinationIsRejected() throws Exception {
        long id = stationId("EKSAR");
        mvc.perform(get("/api/routes").param("from", "" + id).param("to", "" + id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("to"))
                .andExpect(jsonPath("$.fieldErrors[0].message", containsString("different")));
    }

    @Test
    void unknownStationIs404AndBadIdIs400() throws Exception {
        mvc.perform(get("/api/routes").param("from", "999999").param("to", "" + stationId("EKSAR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("999999")));
        mvc.perform(get("/api/routes").param("from", "abc").param("to", "2"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/routes").param("from", "1").param("to", "2").param("preference", "TELEPORT"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void routeResultsPageRenders() throws Exception {
        mvc.perform(get("/routes").param("from", "" + stationId("GOREGAON_WEST")).param("to", "" + stationId("GOREGAON_EAST")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Why this route?")))
                .andExpect(content().string(containsString("Goregaon (East)")));
        mvc.perform(get("/routes").param("from", "1").param("to", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("must be different stations")));
    }
}
