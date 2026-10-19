package com.metropulse.service;

import com.metropulse.entity.AppUser;
import com.metropulse.entity.CrowdReport;
import com.metropulse.enums.CrowdInfoState;
import com.metropulse.enums.CrowdLevel;
import com.metropulse.enums.Role;
import com.metropulse.enums.VerificationStatus;
import com.metropulse.entity.LineStation;
import com.metropulse.entity.MetroLine;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrowdAggregationPolicyTest {

    private static final Instant NOW = Instant.parse("2026-10-19T12:00:00Z");
    private final LineStation node = savedNode();

    /** Entities only get ids from the database, so Mockito mocks stand in for a saved line and node. */
    private static LineStation savedNode() {
        MetroLine line = mock(MetroLine.class);
        when(line.getId()).thenReturn(1L);
        LineStation node = mock(LineStation.class);
        when(node.getId()).thenReturn(4L);
        when(node.getLine()).thenReturn(line);
        return node;
    }

    private final AppUser user = new AppUser("u", "u@x", "hash", Role.COMMUTER);

    private CrowdReport report(CrowdLevel level, int minutesAgo, VerificationStatus status) {
        Instant created = NOW.minus(Duration.ofMinutes(minutesAgo));
        CrowdReport r = new CrowdReport(node, user, level, null, created, created.plus(Duration.ofMinutes(15)));
        if (status != VerificationStatus.PENDING) {
            r.verify(status, user, created);
        }
        return r;
    }

    @Test
    void newestPendingReportWinsWhenNothingIsVerified() {
        var chosen = CrowdAggregationPolicy.representative(List.of(
                report(CrowdLevel.LOW, 10, VerificationStatus.PENDING),
                report(CrowdLevel.HIGH, 2, VerificationStatus.PENDING)), NOW);
        assertThat(chosen).map(CrowdReport::getCrowdLevel).contains(CrowdLevel.HIGH);
    }

    @Test
    void verifiedReportBeatsNewerUnverifiedReport() {
        var chosen = CrowdAggregationPolicy.representative(List.of(
                report(CrowdLevel.MODERATE, 8, VerificationStatus.APPROVED),
                report(CrowdLevel.VERY_HIGH, 1, VerificationStatus.PENDING)), NOW);
        assertThat(chosen).map(CrowdReport::getCrowdLevel).contains(CrowdLevel.MODERATE);
    }

    @Test
    void expiredAndRejectedReportsAreIgnored() {
        var chosen = CrowdAggregationPolicy.representative(List.of(
                report(CrowdLevel.VERY_HIGH, 20, VerificationStatus.APPROVED),   // expired 5 min ago
                report(CrowdLevel.HIGH, 3, VerificationStatus.REJECTED)), NOW);
        assertThat(chosen).isEmpty();
    }

    @Test
    void reportExpiresExactlyAtItsExpiryTime() {
        CrowdReport r = report(CrowdLevel.LOW, 15, VerificationStatus.PENDING);
        assertThat(CrowdAggregationPolicy.isEligible(r, NOW)).isFalse();
        assertThat(CrowdAggregationPolicy.isEligible(r, NOW.minusSeconds(1))).isTrue();
    }

    @Test
    void stationStatusDistinguishesVerifiedReportedStaleAndNoData() {
        assertThat(CrowdStatusService.describe(node, List.of(report(CrowdLevel.LOW, 1, VerificationStatus.APPROVED)), NOW).state())
                .isEqualTo(CrowdInfoState.VERIFIED);
        assertThat(CrowdStatusService.describe(node, List.of(report(CrowdLevel.LOW, 1, VerificationStatus.PENDING)), NOW).state())
                .isEqualTo(CrowdInfoState.USER_REPORTED);
        var stale = CrowdStatusService.describe(node, List.of(report(CrowdLevel.HIGH, 40, VerificationStatus.PENDING)), NOW);
        assertThat(stale.state()).isEqualTo(CrowdInfoState.STALE);
        assertThat(stale.level()).isEqualTo(CrowdLevel.HIGH);
        var none = CrowdStatusService.describe(node, List.of(), NOW);
        assertThat(none.state()).isEqualTo(CrowdInfoState.NO_DATA);
        assertThat(none.level()).isNull(); // missing data is never turned into LOW
    }
}
