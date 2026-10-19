package com.metropulse.service;

import com.metropulse.config.MetroPulseProperties;
import com.metropulse.dto.CrowdReportView;
import com.metropulse.dto.form.CrowdReportForm;
import com.metropulse.entity.AppUser;
import com.metropulse.entity.CrowdReport;
import com.metropulse.entity.LineStation;
import com.metropulse.enums.AuditAction;
import com.metropulse.enums.CrowdLevel;
import com.metropulse.enums.VerificationStatus;
import com.metropulse.exception.InvalidRequestException;
import com.metropulse.exception.InvalidStateTransitionException;
import com.metropulse.exception.RateLimitExceededException;
import com.metropulse.exception.ResourceNotFoundException;
import com.metropulse.repository.CrowdReportRepository;
import com.metropulse.repository.LineStationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Crowd report submission and moderation.
 * <p>
 * Submission policy: only signed-in users can report. Anti-spam rules: one report per user for the same
 * station-line within {@code min-interval-same-station}, and at most {@code max-reports-per-hour} per user.
 * Reports start as PENDING (usable for routing, shown as "user reported") and expire after {@code report-ttl}.
 * A report submitted by an administrator is treated as verified immediately.
 */
@Service
@Transactional(readOnly = true)
public class CrowdReportService {

    private final CrowdReportRepository reports;
    private final LineStationRepository lineStations;
    private final AuditService audit;
    private final MetroPulseProperties.Crowd settings;
    private final Clock clock;

    public CrowdReportService(CrowdReportRepository reports, LineStationRepository lineStations, AuditService audit,
                              MetroPulseProperties properties, Clock clock) {
        this.reports = reports;
        this.lineStations = lineStations;
        this.audit = audit;
        this.settings = properties.crowd();
        this.clock = clock;
    }

    @Transactional
    public CrowdReportView submit(CrowdReportForm form, AppUser user) {
        LineStation node = lineStations.findWithLineAndStation(form.getLineStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station on line", form.getLineStationId()));
        if (!node.isOperational() || !node.getStation().isOpen()) {
            throw new InvalidRequestException(node.getStation().getName() + " currently has no stopping service on "
                    + node.getLine().getShortName());
        }
        Instant now = clock.instant();
        enforceRateLimits(user, node, now);

        String comment = form.getComment() == null || form.getComment().isBlank() ? null : form.getComment().strip();
        CrowdReport report = new CrowdReport(node, user, form.getCrowdLevel(), comment, now, now.plus(settings.reportTtl()));
        if (user.isAdmin()) {
            report.verify(VerificationStatus.APPROVED, user, now);
        }
        reports.save(report);
        return CrowdReportView.of(report, false);
    }

    private void enforceRateLimits(AppUser user, LineStation node, Instant now) {
        Duration interval = settings.minIntervalSameStation();
        if (reports.existsByUserIdAndLineStationIdAndCreatedAtAfter(user.getId(), node.getId(), now.minus(interval))) {
            throw new RateLimitExceededException("You already reported " + node.getStation().getName()
                    + " recently. Please wait " + interval.toMinutes() + " minutes between reports for the same station.");
        }
        if (reports.countByUserIdAndCreatedAtAfter(user.getId(), now.minus(Duration.ofHours(1))) >= settings.maxReportsPerHour()) {
            throw new RateLimitExceededException("You have reached the limit of " + settings.maxReportsPerHour()
                    + " reports per hour. Thank you for helping - please try again later.");
        }
    }

    public Page<CrowdReportView> mine(long userId, int page) {
        return reports.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(page, 0), 10))
                .map(r -> CrowdReportView.of(r, false));
    }

    /** Admin list with server-side filtering and pagination. */
    public Page<CrowdReportView> search(VerificationStatus status, Long lineId, CrowdLevel level, Pageable pageable) {
        Specification<CrowdReport> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("verificationStatus"), status));
        }
        if (lineId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("lineStation").get("line").get("id"), lineId));
        }
        if (level != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("crowdLevel"), level));
        }
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return reports.findAll(spec, sorted).map(r -> CrowdReportView.of(r, true));
    }

    /** Recent non-rejected reports for a station page (newest first). Reporter names are not exposed. */
    public List<CrowdReportView> recentForStation(List<LineStation> nodes, int limit) {
        if (nodes.isEmpty()) {
            return List.of();
        }
        Instant since = clock.instant().minus(Duration.ofHours(24));
        List<Long> nodeIds = nodes.stream().map(LineStation::getId).toList();
        return reports.findRecentForNodes(nodeIds, since).stream()
                .limit(limit)
                .map(r -> CrowdReportView.of(r, false))
                .toList();
    }

    @Transactional
    public CrowdReportView verify(long reportId, VerificationStatus decision, AppUser admin) {
        if (decision != VerificationStatus.APPROVED && decision != VerificationStatus.REJECTED) {
            throw new InvalidRequestException("Decision must be APPROVED or REJECTED");
        }
        CrowdReport report = reports.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Crowd report", reportId));
        if (report.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new InvalidStateTransitionException("Report " + reportId + " was already " + report.getVerificationStatus());
        }
        report.verify(decision, admin, clock.instant());
        audit.record(admin, decision == VerificationStatus.APPROVED ? AuditAction.REPORT_APPROVED : AuditAction.REPORT_REJECTED,
                "CrowdReport", reportId, report.getCrowdLevel() + " at " + report.getLineStation().getStation().getName());
        return CrowdReportView.of(report, true);
    }

    public long pendingCount() {
        return reports.countByVerificationStatus(VerificationStatus.PENDING);
    }
}
