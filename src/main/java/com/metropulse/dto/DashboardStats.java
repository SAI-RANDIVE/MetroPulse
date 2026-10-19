package com.metropulse.dto;

import java.util.List;
import java.util.Map;

/** Numbers for the admin dashboard, all computed from the database. */
public record DashboardStats(long lineCount,
                             long stationCount,
                             long activeAlerts,
                             long draftAlerts,
                             long pendingReports,
                             long reportsLast24h,
                             Map<String, Long> crowdDistributionLast7Days,
                             List<StationCount> mostReportedStationsLast7Days,
                             int graphNodes,
                             int graphEdges,
                             List<String> networkIssues) {

    public record StationCount(long stationId, String stationName, long reports) {
    }

    /* Chart data for the dashboard template, joined with "|" into data attributes (no inline script needed). */

    public String crowdLabels() {
        return String.join("|", crowdDistributionLast7Days.keySet());
    }

    public String crowdValues() {
        return join(crowdDistributionLast7Days.values().stream().map(String::valueOf).toList());
    }

    public String topStationLabels() {
        return join(mostReportedStationsLast7Days.stream().map(StationCount::stationName).toList());
    }

    public String topStationValues() {
        return join(mostReportedStationsLast7Days.stream().map(c -> String.valueOf(c.reports())).toList());
    }

    private static String join(List<String> parts) {
        return String.join("|", parts);
    }
}
