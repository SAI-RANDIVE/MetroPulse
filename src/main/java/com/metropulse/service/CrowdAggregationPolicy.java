package com.metropulse.service;

import com.metropulse.entity.CrowdReport;
import com.metropulse.enums.VerificationStatus;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Decides which report represents the current crowd level when several exist for one station on one line.
 * <p>
 * Policy ("latest verified, otherwise latest reported"):
 * <ol>
 *     <li>Only eligible reports count: not REJECTED and not expired.</li>
 *     <li>If any eligible report was APPROVED by an administrator, the most recent approved one wins.</li>
 *     <li>Otherwise the most recent PENDING (commuter-reported) report wins.</li>
 * </ol>
 * Verified information is preferred because a moderator has checked it; within the same
 * verification level the newest report is preferred because crowding changes quickly.
 */
public final class CrowdAggregationPolicy {

    private CrowdAggregationPolicy() {
    }

    public static boolean isEligible(CrowdReport report, Instant now) {
        return report.getVerificationStatus() != VerificationStatus.REJECTED && !report.isExpiredAt(now);
    }

    public static Optional<CrowdReport> representative(List<CrowdReport> reports, Instant now) {
        Comparator<CrowdReport> newestFirst = Comparator.comparing(CrowdReport::getCreatedAt).reversed();
        List<CrowdReport> eligible = reports.stream().filter(r -> isEligible(r, now)).sorted(newestFirst).toList();
        return eligible.stream()
                .filter(r -> r.getVerificationStatus() == VerificationStatus.APPROVED)
                .findFirst()
                .or(() -> eligible.stream().findFirst());
    }
}
