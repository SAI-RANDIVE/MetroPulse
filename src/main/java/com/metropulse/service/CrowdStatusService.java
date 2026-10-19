package com.metropulse.service;

import com.metropulse.config.MetroPulseProperties;
import com.metropulse.dto.CrowdStatusView;
import com.metropulse.dto.LineBadge;
import com.metropulse.entity.CrowdReport;
import com.metropulse.entity.LineStation;
import com.metropulse.enums.CrowdInfoState;
import com.metropulse.enums.CrowdLevel;
import com.metropulse.enums.VerificationStatus;
import com.metropulse.repository.CrowdReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Reads crowd reports and turns them into "what do we know right now" for routing and station pages. */
@Service
@Transactional(readOnly = true)
public class CrowdStatusService {

    private final CrowdReportRepository reports;
    private final MetroPulseProperties.Crowd settings;
    private final Clock clock;

    public CrowdStatusService(CrowdReportRepository reports, MetroPulseProperties properties, Clock clock) {
        this.reports = reports;
        this.settings = properties.crowd();
        this.clock = clock;
    }

    /** Current crowd level per line-station node. Nodes without an eligible report are absent (not LOW). */
    public Map<Long, CrowdLevel> currentLevels() {
        Instant now = clock.instant();
        Map<Long, List<CrowdReport>> byNode = reports.findEligible(now).stream()
                .collect(Collectors.groupingBy(r -> r.getLineStation().getId()));
        Map<Long, CrowdLevel> levels = new HashMap<>();
        byNode.forEach((nodeId, nodeReports) -> CrowdAggregationPolicy.representative(nodeReports, now)
                .ifPresent(r -> levels.put(nodeId, r.getCrowdLevel())));
        return levels;
    }

    /** Status of each given node (all nodes of one station, typically). Nodes must have their line loaded. */
    public List<CrowdStatusView> statusFor(List<LineStation> nodes) {
        Instant now = clock.instant();
        if (nodes.isEmpty()) {
            return List.of();
        }
        List<Long> ids = nodes.stream().map(LineStation::getId).toList();
        Map<Long, List<CrowdReport>> recent = reports.findRecentForNodes(ids, now.minus(settings.staleWindow())).stream()
                .collect(Collectors.groupingBy(r -> r.getLineStation().getId()));

        List<CrowdStatusView> result = new ArrayList<>();
        for (LineStation node : nodes) {
            List<CrowdReport> nodeReports = recent.getOrDefault(node.getId(), List.of());
            result.add(describe(node, nodeReports, now));
        }
        return result;
    }

    static CrowdStatusView describe(LineStation node, List<CrowdReport> nodeReports, Instant now) {
        LineBadge line = LineBadge.of(node.getLine());
        int current = (int) nodeReports.stream().filter(r -> CrowdAggregationPolicy.isEligible(r, now)).count();
        var representative = CrowdAggregationPolicy.representative(nodeReports, now);
        if (representative.isPresent()) {
            CrowdReport r = representative.get();
            CrowdInfoState state = r.getVerificationStatus() == VerificationStatus.APPROVED
                    ? CrowdInfoState.VERIFIED : CrowdInfoState.USER_REPORTED;
            return new CrowdStatusView(node.getId(), line, state, r.getCrowdLevel(), r.getCreatedAt(), r.getExpiresAt(), current);
        }
        // nodeReports is newest first; anything left is expired but still inside the stale window
        return nodeReports.stream()
                .filter(r -> r.getVerificationStatus() != VerificationStatus.REJECTED)
                .findFirst()
                .map(r -> new CrowdStatusView(node.getId(), line, CrowdInfoState.STALE, r.getCrowdLevel(),
                        r.getCreatedAt(), r.getExpiresAt(), 0))
                .orElseGet(() -> new CrowdStatusView(node.getId(), line, CrowdInfoState.NO_DATA, null, null, null, 0));
    }
}
