package com.metropulse.dto;

import com.metropulse.enums.StationStatus;

import java.util.List;

/** Station list item: used by the directory page, autocomplete and GET /api/stations. */
public record StationSummary(long id, String code, String name, String area, StationStatus status, List<LineBadge> lines) {

    public boolean isInterchange() {
        return lines.size() > 1;
    }
}
