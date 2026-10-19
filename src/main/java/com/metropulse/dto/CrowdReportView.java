package com.metropulse.dto;

import com.metropulse.entity.CrowdReport;
import com.metropulse.enums.CrowdLevel;
import com.metropulse.enums.VerificationStatus;

import java.time.Instant;

/** A crowd report without personal data (the reporter is shown by display name only to admins). */
public record CrowdReportView(long id, long lineStationId, String stationName, long stationId, LineBadge line,
                              CrowdLevel level, String comment, VerificationStatus status,
                              Instant createdAt, Instant expiresAt, String reporterName, String verifiedByName,
                              Instant verifiedAt, boolean demo) {

    /** Requires lineStation, its line and station to be loaded; reporter names only if requested. */
    public static CrowdReportView of(CrowdReport r, boolean includeNames) {
        var ls = r.getLineStation();
        return new CrowdReportView(r.getId(), ls.getId(), ls.getStation().getName(), ls.getStation().getId(),
                LineBadge.of(ls.getLine()), r.getCrowdLevel(), r.getComment(), r.getVerificationStatus(),
                r.getCreatedAt(), r.getExpiresAt(),
                includeNames ? (r.getUser() == null ? "Demo data" : r.getUser().getDisplayName()) : null,
                includeNames && r.getVerifiedBy() != null ? r.getVerifiedBy().getDisplayName() : null,
                r.getVerifiedAt(), r.isDemo());
    }

    public boolean isExpiredAt(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
