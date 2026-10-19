package com.metropulse.service;

import com.metropulse.repository.LineStationRepository;
import com.metropulse.repository.NetworkEdgeRepository;
import com.metropulse.service.routing.MetroGraph;
import com.metropulse.service.routing.MetroGraphBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the routing graph from PostgreSQL. The network is small (about 45 nodes and 90 edges),
 * so it is rebuilt per request - admin changes take effect immediately and no cache can go stale.
 */
@Service
public class NetworkGraphService {

    private static final Logger log = LoggerFactory.getLogger(NetworkGraphService.class);

    private final LineStationRepository lineStations;
    private final NetworkEdgeRepository edges;

    public NetworkGraphService(LineStationRepository lineStations, NetworkEdgeRepository edges) {
        this.lineStations = lineStations;
        this.edges = edges;
    }

    @Transactional(readOnly = true)
    public MetroGraph currentGraph() {
        MetroGraph graph = MetroGraphBuilder.fromEntities(lineStations.findAllWithLineAndStation(), edges.findAllWithNodes());
        if (!graph.issues().isEmpty()) {
            log.warn("Routing graph built with {} data issue(s); invalid edges were excluded", graph.issues().size());
        }
        return graph;
    }
}
