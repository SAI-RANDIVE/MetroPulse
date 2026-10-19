package com.metropulse.service;

import com.metropulse.entity.CrowdReport;
import com.metropulse.entity.LineStation;
import com.metropulse.enums.CrowdLevel;
import com.metropulse.enums.VerificationStatus;
import com.metropulse.repository.CrowdReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** CrowdStatusService with a mocked repository and a fixed clock (Mockito). */
@ExtendWith(MockitoExtension.class)
class CrowdStatusServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T12:00:00Z");

    @Mock
    CrowdReportRepository repository;

    private CrowdReport mockReport(long nodeId, CrowdLevel level, VerificationStatus status, int minutesAgo) {
        LineStation node = mock(LineStation.class);
        when(node.getId()).thenReturn(nodeId);
        CrowdReport r = mock(CrowdReport.class);
        when(r.getLineStation()).thenReturn(node);
        lenient().when(r.getCrowdLevel()).thenReturn(level);
        lenient().when(r.getVerificationStatus()).thenReturn(status);
        lenient().when(r.getCreatedAt()).thenReturn(NOW.minus(Duration.ofMinutes(minutesAgo)));
        lenient().when(r.isExpiredAt(NOW)).thenReturn(false);
        return r;
    }

    @Test
    void currentLevelsAggregatesPerNodeUsingThePolicy() {
        // build the mocks first: Mockito does not allow creating stubs inside another when(...)
        List<CrowdReport> eligible = List.of(
                mockReport(1, CrowdLevel.LOW, VerificationStatus.PENDING, 1),
                mockReport(1, CrowdLevel.HIGH, VerificationStatus.APPROVED, 9),
                mockReport(2, CrowdLevel.MODERATE, VerificationStatus.PENDING, 3));
        when(repository.findEligible(NOW)).thenReturn(eligible);
        var service = new CrowdStatusService(repository,
                com.metropulse.service.routing.TestNetworksAccess.properties(), Clock.fixed(NOW, ZoneOffset.UTC));

        var levels = service.currentLevels();

        assertThat(levels).containsEntry(1L, CrowdLevel.HIGH).containsEntry(2L, CrowdLevel.MODERATE).hasSize(2);
        verify(repository).findEligible(NOW);
    }

    @Test
    void noEligibleReportsMeansNoLevels() {
        when(repository.findEligible(NOW)).thenReturn(List.of());
        var service = new CrowdStatusService(repository,
                com.metropulse.service.routing.TestNetworksAccess.properties(), Clock.fixed(NOW, ZoneOffset.UTC));
        assertThat(service.currentLevels()).isEmpty();
    }
}
