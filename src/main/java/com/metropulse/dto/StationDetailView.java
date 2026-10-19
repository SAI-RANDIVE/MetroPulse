package com.metropulse.dto;

import java.util.List;

/** Everything on the station page. */
public record StationDetailView(StationSummary station,
                                List<Membership> memberships,
                                List<Interchange> interchanges,
                                StationStatusView status,
                                List<CrowdReportView> recentReports) {

    /** The station's place on one line, with its neighbours. */
    public record Membership(long lineStationId, LineBadge line, int sequenceNo, boolean operational,
                             StationRef previous, StationRef next) {
    }

    /** A modelled transfer from this station to another line (possibly at a neighbouring station). */
    public record Interchange(StationRef toStation, LineBadge fromLine, LineBadge toLine, int minutes, String note) {
    }
}
