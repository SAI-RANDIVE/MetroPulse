package com.metropulse.dto;

import com.metropulse.enums.CrowdInfoState;
import com.metropulse.enums.CrowdLevel;

import java.time.Instant;

/**
 * Crowd information for one station on one line.
 * For STALE the level is the last (expired) report; for NO_DATA the level is null.
 */
public record CrowdStatusView(long lineStationId, LineBadge line, CrowdInfoState state, CrowdLevel level,
                              Instant reportedAt, Instant expiresAt, int currentReportCount) {
}
