package com.metropulse.dto;

import com.metropulse.enums.LineStatus;

import java.util.List;

/** A metro line with its stops in order (GET /api/lines and the line pages). */
public record LineSummary(long id, String code, String name, String color, String textColor,
                          String operatorName, LineStatus status, List<LineStop> stops) {

    public record LineStop(long lineStationId, long stationId, String stationName, int sequenceNo,
                           boolean operational, List<LineBadge> otherLines) {
        public boolean isInterchange() {
            return !otherLines.isEmpty();
        }
    }

    public String firstStop() {
        return stops.isEmpty() ? "" : stops.getFirst().stationName();
    }

    public String lastStop() {
        return stops.isEmpty() ? "" : stops.getLast().stationName();
    }
}
