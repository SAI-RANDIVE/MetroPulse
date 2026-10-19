package com.metropulse.service;

import com.metropulse.config.MetroPulseProperties;
import com.metropulse.dto.AlertSummary;
import com.metropulse.dto.form.AlertForm;
import com.metropulse.entity.*;
import com.metropulse.enums.*;
import com.metropulse.exception.InvalidRequestException;
import com.metropulse.exception.InvalidStateTransitionException;
import com.metropulse.exception.ResourceNotFoundException;
import com.metropulse.repository.LineStationRepository;
import com.metropulse.repository.MetroLineRepository;
import com.metropulse.repository.NetworkEdgeRepository;
import com.metropulse.repository.ServiceAlertRepository;
import com.metropulse.repository.StationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Service alerts: creation, editing, the DRAFT -> ACTIVE -> RESOLVED / CANCELLED workflow,
 * and which alerts are "in effect" for commuters and the route planner.
 * Activating an alert records the administrator as its verifier.
 */
@Service
@Transactional(readOnly = true)
public class ServiceAlertService {

    private final ServiceAlertRepository alerts;
    private final MetroLineRepository lines;
    private final StationRepository stations;
    private final LineStationRepository lineStations;
    private final NetworkEdgeRepository edges;
    private final AuditService audit;
    private final Clock clock;
    private final ZoneId zone;

    public ServiceAlertService(ServiceAlertRepository alerts, MetroLineRepository lines, StationRepository stations,
                               LineStationRepository lineStations, NetworkEdgeRepository edges, AuditService audit,
                               Clock clock, MetroPulseProperties properties) {
        this.alerts = alerts;
        this.lines = lines;
        this.stations = stations;
        this.lineStations = lineStations;
        this.edges = edges;
        this.audit = audit;
        this.clock = clock;
        this.zone = properties.timeZone();
    }

    // ---------- reading ----------

    /** Active, verified alerts whose time window includes now. Entities have line, station and edges loaded. */
    public List<ServiceAlert> inEffect() {
        Instant now = clock.instant();
        return alerts.findByStatus(AlertStatus.ACTIVE).stream()
                .filter(a -> a.isInEffectAt(now))
                .sorted(Comparator.comparing(ServiceAlert::getSeverity).reversed()
                        .thenComparing(ServiceAlert::getStartsAt, Comparator.reverseOrder()))
                .toList();
    }

    public List<AlertSummary> publicActive() {
        return inEffect().stream().map(AlertSummary::of).toList();
    }

    public List<AlertSummary> recentlyResolved() {
        return alerts.findTop5ByStatusOrderByUpdatedAtDesc(AlertStatus.RESOLVED).stream()
                .map(AlertSummary::withoutEdges).toList();
    }

    /** Public lookup: drafts and never-verified alerts are hidden. */
    public AlertSummary publicGet(long id) {
        ServiceAlert alert = alerts.findDetailed(id)
                .filter(a -> a.getVerifiedAt() != null
                        && (a.getStatus() == AlertStatus.ACTIVE || a.getStatus() == AlertStatus.RESOLVED))
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
        return AlertSummary.of(alert);
    }

    public AlertSummary adminGet(long id) {
        return AlertSummary.of(require(id));
    }

    public Page<AlertSummary> adminPage(AlertStatus status, Pageable pageable) {
        Page<ServiceAlert> page = status == null ? alerts.findAllWithRefs(pageable) : alerts.findByStatus(status, pageable);
        return page.map(AlertSummary::withoutEdges);
    }

    /** In-effect alerts relevant to one station: aimed at it, at a connection touching it, or line-wide on its lines. */
    public List<AlertSummary> affectingStation(long stationId, Collection<Long> lineIds, Collection<Long> nodeIds) {
        return inEffect().stream().filter(a -> {
            if (a.getStation() != null) {
                return a.getStation().getId() == stationId;
            }
            if (!a.getAffectedEdges().isEmpty()) {
                return a.getAffectedEdges().stream().anyMatch(e ->
                        nodeIds.contains(e.getFromNode().getId()) || nodeIds.contains(e.getToNode().getId()));
            }
            return a.getLine() != null && lineIds.contains(a.getLine().getId());
        }).map(AlertSummary::of).toList();
    }

    public long countByStatus(AlertStatus status) {
        return alerts.countByStatus(status);
    }

    /** Builds an edit form from an existing alert, including the segment end stations if any. */
    public AlertForm toForm(long id) {
        ServiceAlert a = require(id);
        AlertForm form = new AlertForm();
        form.setLineId(a.getLine() == null ? null : a.getLine().getId());
        form.setStationId(a.getStation() == null ? null : a.getStation().getId());
        form.setTitle(a.getTitle());
        form.setDescription(a.getDescription());
        form.setAlertType(a.getAlertType());
        form.setSeverity(a.getSeverity());
        form.setImpact(a.getImpact());
        form.setDelayMinutes(a.getDelayMinutes());
        form.setStartsAt(LocalDateTime.ofInstant(a.getStartsAt(), zone));
        form.setEndsAt(a.getEndsAt() == null ? null : LocalDateTime.ofInstant(a.getEndsAt(), zone));
        if (!a.getAffectedEdges().isEmpty()) {
            var nodes = a.getAffectedEdges().stream()
                    .flatMap(e -> java.util.stream.Stream.of(e.getFromNode(), e.getToNode())).toList();
            nodes.stream().min(Comparator.comparingInt(LineStation::getSequenceNo))
                    .ifPresent(n -> form.setSegmentFromStationId(n.getStation().getId()));
            nodes.stream().max(Comparator.comparingInt(LineStation::getSequenceNo))
                    .ifPresent(n -> form.setSegmentToStationId(n.getStation().getId()));
        }
        return form;
    }

    // ---------- writing ----------

    @Transactional
    public AlertSummary create(AlertForm form, AppUser admin) {
        ServiceAlert alert = new ServiceAlert();
        alert.setCreatedBy(admin);
        applyForm(alert, form);
        alerts.save(alert);
        audit.record(admin, AuditAction.ALERT_CREATED, "ServiceAlert", alert.getId(), alert.getTitle());
        if (form.isPublishNow()) {
            activate(alert, admin);
            audit.record(admin, AuditAction.ALERT_STATUS_CHANGED, "ServiceAlert", alert.getId(), "DRAFT -> ACTIVE");
        }
        return AlertSummary.of(alert);
    }

    @Transactional
    public AlertSummary update(long id, AlertForm form, AppUser admin) {
        ServiceAlert alert = require(id);
        if (!alert.getStatus().isEditable()) {
            throw new InvalidStateTransitionException("A " + alert.getStatus() + " alert can no longer be edited");
        }
        applyForm(alert, form);
        if (alert.getStatus() == AlertStatus.ACTIVE) {
            activate(alert, admin); // the editor re-verifies the changed content
        }
        audit.record(admin, AuditAction.ALERT_UPDATED, "ServiceAlert", id, alert.getTitle());
        return AlertSummary.of(alert);
    }

    @Transactional
    public AlertSummary transition(long id, AlertStatus target, AppUser admin) {
        ServiceAlert alert = require(id);
        AlertStatus from = alert.getStatus();
        if (!from.canTransitionTo(target)) {
            throw new InvalidStateTransitionException("Cannot change an alert from " + from + " to " + target
                    + (from.allowedNext().isEmpty() ? " (" + from + " is final)" : "; allowed: " + from.allowedNext()));
        }
        Instant now = clock.instant();
        switch (target) {
            case ACTIVE -> activate(alert, admin);
            case RESOLVED -> {
                alert.setStatus(AlertStatus.RESOLVED);
                if (alert.getEndsAt() == null || alert.getEndsAt().isAfter(now)) {
                    alert.setEndsAt(now.isAfter(alert.getStartsAt()) ? now : alert.getStartsAt().plusSeconds(60));
                }
            }
            case CANCELLED -> alert.setStatus(AlertStatus.CANCELLED);
            case DRAFT -> throw new IllegalStateException("unreachable: no transition leads back to DRAFT");
        }
        audit.record(admin, AuditAction.ALERT_STATUS_CHANGED, "ServiceAlert", id, from + " -> " + target);
        return AlertSummary.of(alert);
    }

    private void activate(ServiceAlert alert, AppUser admin) {
        alert.setStatus(AlertStatus.ACTIVE);
        alert.setVerifiedBy(admin);
        alert.setVerifiedAt(clock.instant());
    }

    /** Validates the form against the network and copies it onto the alert (never touches status or users). */
    private void applyForm(ServiceAlert alert, AlertForm form) {
        MetroLine line = form.getLineId() == null ? null
                : lines.findById(form.getLineId()).orElseThrow(() -> new ResourceNotFoundException("Line", form.getLineId()));
        Station station = form.getStationId() == null ? null
                : stations.findById(form.getStationId()).orElseThrow(() -> new ResourceNotFoundException("Station", form.getStationId()));
        if (line == null && station == null) {
            throw new InvalidRequestException("Choose the affected line, station or both");
        }
        if (line != null && station != null && !lineStations.existsByLineIdAndStationId(line.getId(), station.getId())) {
            throw new InvalidRequestException(station.getName() + " is not on " + line.getShortName());
        }

        Set<NetworkEdge> segment = resolveSegment(line, form);
        if (form.getImpact() == AlertImpact.DELAY) {
            if (form.getDelayMinutes() <= 0) {
                throw new InvalidRequestException("A delay alert needs the expected delay in minutes");
            }
            if (segment.isEmpty() && station == null) {
                throw new InvalidRequestException("A delay alert needs an affected station or line segment");
            }
        }

        Instant startsAt = form.getStartsAt().atZone(zone).toInstant();
        Instant endsAt = form.getEndsAt() == null ? null : form.getEndsAt().atZone(zone).toInstant();
        if (endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new InvalidRequestException("The end time must be after the start time");
        }

        alert.setLine(line);
        alert.setStation(station);
        alert.setTitle(form.getTitle().strip());
        alert.setDescription(form.getDescription().strip());
        alert.setAlertType(form.getAlertType());
        alert.setSeverity(form.getSeverity());
        alert.setImpact(form.getImpact());
        alert.setDelayMinutes(form.getImpact() == AlertImpact.DELAY ? form.getDelayMinutes() : 0);
        alert.setStartsAt(startsAt);
        alert.setEndsAt(endsAt);
        alert.getAffectedEdges().clear();
        alert.getAffectedEdges().addAll(segment);
    }

    /** Both directions of every ride connection between the two segment stations on the chosen line. */
    private Set<NetworkEdge> resolveSegment(MetroLine line, AlertForm form) {
        Long fromId = form.getSegmentFromStationId();
        Long toId = form.getSegmentToStationId();
        if (fromId == null && toId == null) {
            return Set.of();
        }
        if (fromId == null || toId == null || line == null) {
            throw new InvalidRequestException("A line segment needs the line plus both end stations");
        }
        if (fromId.equals(toId)) {
            throw new InvalidRequestException("A line segment needs two different end stations");
        }
        LineStation a = lineStations.findByLineIdAndStationId(line.getId(), fromId)
                .orElseThrow(() -> new InvalidRequestException("Segment start station is not on " + line.getShortName()));
        LineStation b = lineStations.findByLineIdAndStationId(line.getId(), toId)
                .orElseThrow(() -> new InvalidRequestException("Segment end station is not on " + line.getShortName()));
        int low = Math.min(a.getSequenceNo(), b.getSequenceNo());
        int high = Math.max(a.getSequenceNo(), b.getSequenceNo());
        return Set.copyOf(edges.findRideEdgesInSegment(line.getId(), low, high));
    }

    private ServiceAlert require(long id) {
        return alerts.findDetailed(id).orElseThrow(() -> new ResourceNotFoundException("Alert", id));
    }
}
