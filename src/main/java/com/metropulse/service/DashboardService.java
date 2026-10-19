package com.metropulse.service;

import com.metropulse.dto.DashboardStats;
import com.metropulse.enums.AlertStatus;
import com.metropulse.enums.CrowdLevel;
import com.metropulse.enums.VerificationStatus;
import com.metropulse.repository.CrowdReportRepository;
import com.metropulse.repository.MetroLineRepository;
import com.metropulse.repository.ServiceAlertRepository;
import com.metropulse.repository.StationRepository;
import com.metropulse.service.routing.MetroGraph;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Admin dashboard figures, all queried from the database (no hard-coded chart values). */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final MetroLineRepository lines;
    private final StationRepository stations;
    private final ServiceAlertRepository alerts;
    private final CrowdReportRepository reports;
    private final NetworkGraphService graphs;
    private final Clock clock;

    public DashboardService(MetroLineRepository lines, StationRepository stations, ServiceAlertRepository alerts,
                            CrowdReportRepository reports, NetworkGraphService graphs, Clock clock) {
        this.lines = lines;
        this.stations = stations;
        this.alerts = alerts;
        this.reports = reports;
        this.graphs = graphs;
        this.clock = clock;
    }

    public DashboardStats stats() {
        Instant now = clock.instant();
        Instant weekAgo = now.minus(Duration.ofDays(7));

        Map<String, Long> distribution = new LinkedHashMap<>();
        for (CrowdLevel level : CrowdLevel.values()) {
            distribution.put(level.name(), 0L);
        }
        for (Object[] row : reports.countByLevelSince(weekAgo)) {
            distribution.put(((CrowdLevel) row[0]).name(), (Long) row[1]);
        }

        List<DashboardStats.StationCount> top = reports.topStationsSince(weekAgo, PageRequest.of(0, 6)).stream()
                .map(row -> new DashboardStats.StationCount((Long) row[0], (String) row[1], (Long) row[2]))
                .toList();

        MetroGraph graph = graphs.currentGraph();
        return new DashboardStats(lines.count(), stations.count(),
                alerts.countByStatus(AlertStatus.ACTIVE), alerts.countByStatus(AlertStatus.DRAFT),
                reports.countByVerificationStatus(VerificationStatus.PENDING),
                reports.countByCreatedAtAfter(now.minus(Duration.ofHours(24))),
                distribution, top, graph.nodeCount(), graph.edgeCount(), graph.issues());
    }
}
