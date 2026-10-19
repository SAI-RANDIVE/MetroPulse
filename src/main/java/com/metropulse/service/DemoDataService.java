package com.metropulse.service;

import com.metropulse.config.MetroPulseProperties;
import com.metropulse.entity.*;
import com.metropulse.enums.*;
import com.metropulse.exception.InvalidRequestException;
import com.metropulse.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Clearly labelled demonstration records so every workflow can be tried on first run:
 * crowd reports (including an expired and a rejected one), alerts (active, resolved, draft) and facilities.
 * <p>
 * Every record has is_demo = true and "(demo)" in its text. Seeding runs only when no demo data exists,
 * and reset deletes ONLY is_demo rows - real user reports, alerts and facilities are never touched.
 * The network itself (lines, stations, connections) is seeded separately by Flyway.
 */
@Service
public class DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);
    static final String DEMO_SOURCE = "Demonstration data - not verified on site";

    private final MetroLineRepository lines;
    private final StationRepository stations;
    private final LineStationRepository lineStations;
    private final NetworkEdgeRepository edges;
    private final CrowdReportRepository reports;
    private final ServiceAlertRepository alerts;
    private final StationFacilityRepository facilities;
    private final AppUserRepository users;
    private final AuditService audit;
    private final MetroPulseProperties properties;
    private final Clock clock;

    public DemoDataService(MetroLineRepository lines, StationRepository stations, LineStationRepository lineStations,
                           NetworkEdgeRepository edges, CrowdReportRepository reports, ServiceAlertRepository alerts,
                           StationFacilityRepository facilities, AppUserRepository users, AuditService audit,
                           MetroPulseProperties properties, Clock clock) {
        this.lines = lines;
        this.stations = stations;
        this.lineStations = lineStations;
        this.edges = edges;
        this.reports = reports;
        this.alerts = alerts;
        this.facilities = facilities;
        this.users = users;
        this.audit = audit;
        this.properties = properties;
        this.clock = clock;
    }

    public boolean resetAllowed() {
        return !properties.isProduction();
    }

    @Transactional
    public boolean seedIfEmpty() {
        if (reports.existsByDemoTrue() || alerts.existsByDemoTrue() || facilities.existsByDemoTrue()) {
            return false;
        }
        seed();
        return true;
    }

    /** Deletes only demonstration rows, then seeds fresh ones (useful because demo crowd reports expire). */
    @Transactional
    public void reset(AppUser admin) {
        if (!resetAllowed()) {
            throw new InvalidRequestException("Demo data reset is disabled in production");
        }
        int removedReports = reports.deleteDemo();
        var demoAlerts = alerts.findByDemoTrue();
        alerts.deleteAll(demoAlerts);
        int removedFacilities = facilities.deleteDemo();
        seed();
        audit.record(admin, AuditAction.DEMO_DATA_RESET, "Demo", null,
                "Removed " + removedReports + " reports, " + demoAlerts.size() + " alerts, " + removedFacilities + " facilities; reseeded");
    }

    private void seed() {
        Instant now = clock.instant();
        AppUser admin = properties.security().adminEmail() == null ? null
                : users.findByEmailIgnoreCase(properties.security().adminEmail()).orElse(null);
        seedCrowdReports(now, admin);
        seedAlerts(now, admin);
        seedFacilities();
        log.info("Demonstration data seeded (all rows flagged is_demo)");
    }

    private void seedCrowdReports(Instant now, AppUser admin) {
        Duration ttl = properties.crowd().reportTtl();
        demoReport("1", "ANDHERI", CrowdLevel.VERY_HIGH, "Demo: long queues at the ticket gates", now.minusSeconds(60), ttl)
                .ifPresent(r -> r.verify(VerificationStatus.APPROVED, admin, now));
        demoReport("1", "WEH", CrowdLevel.HIGH, "Demo: crowded platform towards Ghatkopar", now.minusSeconds(120), ttl);
        demoReport("2A", "ANDHERI_WEST", CrowdLevel.MODERATE, "Demo report", now.minusSeconds(90), ttl)
                .ifPresent(r -> r.verify(VerificationStatus.APPROVED, admin, now));
        demoReport("1", "GHATKOPAR", CrowdLevel.HIGH, "Demo: busy evening interchange", now.minusSeconds(180), ttl);
        demoReport("7", "DAHISAR_EAST", CrowdLevel.LOW, "Demo report", now.minusSeconds(150), ttl);
        demoReport("7", "GOREGAON_EAST", CrowdLevel.LOW, null, now.minusSeconds(30), ttl);
        // Expired two hours ago - shown as stale, ignored by routing
        demoReport("2A", "MALAD_WEST", CrowdLevel.HIGH, "Demo: expired report", now.minus(Duration.ofHours(2)), ttl);
        // Rejected by a moderator - never used
        demoReport("2A", "EKSAR", CrowdLevel.VERY_HIGH, "Demo: rejected report", now.minusSeconds(240), ttl)
                .ifPresent(r -> r.verify(VerificationStatus.REJECTED, admin, now));
    }

    private Optional<CrowdReport> demoReport(String lineCode, String stationCode, CrowdLevel level, String comment,
                                             Instant createdAt, Duration ttl) {
        return node(lineCode, stationCode).map(node -> {
            CrowdReport report = new CrowdReport(node, null, level, comment, createdAt, createdAt.plus(ttl));
            report.setDemo(true);
            return reports.save(report);
        });
    }

    private void seedAlerts(Instant now, AppUser admin) {
        ServiceAlert lift = demoAlert("Lift maintenance at Ghatkopar (demo)",
                "Demonstration alert: one lift at Ghatkopar is under maintenance. Escalators remain available. "
                        + "This is sample data, not a message from the metro operator.",
                AlertType.FACILITY_ISSUE, AlertSeverity.WARNING, AlertImpact.INFORMATION, 0,
                now.minus(Duration.ofHours(1)), now.plus(Duration.ofDays(1)), admin);
        lines.findByLineCodeIgnoreCase("1").ifPresent(lift::setLine);
        stations.findByCode("GHATKOPAR").ifPresent(lift::setStation);
        verify(lift, admin, now);
        alerts.save(lift);

        ServiceAlert resolved = demoAlert("Signal fault between Malad (West) and Goregaon (West) (demo)",
                "Demonstration alert: trains ran about 10 minutes late on this segment. Now resolved. Sample data only.",
                AlertType.SERVICE_DISRUPTION, AlertSeverity.WARNING, AlertImpact.DELAY, 10,
                now.minus(Duration.ofDays(3)), now.minus(Duration.ofDays(3)).plus(Duration.ofHours(2)), admin);
        attachSegment(resolved, "2A", "MALAD_WEST", "GOREGAON_WEST");
        verify(resolved, admin, now.minus(Duration.ofDays(3)));
        resolved.setStatus(AlertStatus.RESOLVED);
        alerts.save(resolved);

        ServiceAlert draft = demoAlert("Planned maintenance: Eksar to Borivali (West) (demo)",
                "Demonstration draft: activate it from the admin alerts page to see the route planner avoid "
                        + "this suspended Line 2A segment. Sample data only.",
                AlertType.SERVICE_DISRUPTION, AlertSeverity.CRITICAL, AlertImpact.SUSPENDED, 0,
                now, null, admin);
        attachSegment(draft, "2A", "EKSAR", "BORIVALI_WEST");
        alerts.save(draft);
    }

    private ServiceAlert demoAlert(String title, String description, AlertType type, AlertSeverity severity,
                                   AlertImpact impact, int delay, Instant startsAt, Instant endsAt, AppUser admin) {
        ServiceAlert alert = new ServiceAlert();
        alert.setTitle(title);
        alert.setDescription(description);
        alert.setAlertType(type);
        alert.setSeverity(severity);
        alert.setImpact(impact);
        alert.setDelayMinutes(delay);
        alert.setStartsAt(startsAt);
        alert.setEndsAt(endsAt);
        alert.setCreatedBy(admin);
        alert.setStatus(AlertStatus.DRAFT);
        alert.setDemo(true);
        return alert;
    }

    private static void verify(ServiceAlert alert, AppUser admin, Instant when) {
        alert.setStatus(AlertStatus.ACTIVE);
        alert.setVerifiedBy(admin);
        alert.setVerifiedAt(when);
    }

    private void attachSegment(ServiceAlert alert, String lineCode, String fromCode, String toCode) {
        Optional<LineStation> a = node(lineCode, fromCode);
        Optional<LineStation> b = node(lineCode, toCode);
        if (a.isEmpty() || b.isEmpty()) {
            return;
        }
        MetroLine line = a.get().getLine();
        alert.setLine(line);
        int low = Math.min(a.get().getSequenceNo(), b.get().getSequenceNo());
        int high = Math.max(a.get().getSequenceNo(), b.get().getSequenceNo());
        alert.getAffectedEdges().addAll(edges.findRideEdgesInSegment(line.getId(), low, high));
    }

    private void seedFacilities() {
        facility("ANDHERI", FacilityType.LIFT, FacilityStatus.AVAILABLE, "Demo: lift from concourse to platforms");
        facility("ANDHERI", FacilityType.ESCALATOR, FacilityStatus.AVAILABLE, null);
        facility("DN_NAGAR", FacilityType.LIFT, FacilityStatus.AVAILABLE, null);
        facility("ANDHERI_WEST", FacilityType.LIFT, FacilityStatus.AVAILABLE, null);
        facility("WEH", FacilityType.LIFT, FacilityStatus.AVAILABLE, null);
        facility("GUNDAVALI", FacilityType.LIFT, FacilityStatus.AVAILABLE, null);
        facility("GHATKOPAR", FacilityType.LIFT, FacilityStatus.OUT_OF_SERVICE, "Demo: under maintenance");
        facility("GHATKOPAR", FacilityType.ESCALATOR, FacilityStatus.AVAILABLE, null);
        facility("DAHISAR_EAST", FacilityType.LIFT, FacilityStatus.AVAILABLE, null);
        facility("DAHISAR_EAST", FacilityType.PARKING, FacilityStatus.UNKNOWN, "Demo: availability not confirmed");
        facility("GOREGAON_WEST", FacilityType.RESTROOM, FacilityStatus.AVAILABLE, null);
    }

    private void facility(String stationCode, FacilityType type, FacilityStatus status, String description) {
        stations.findByCode(stationCode).ifPresent(station -> {
            boolean realRecordExists = facilities.findByStationIdOrderByFacilityType(station.getId()).stream()
                    .anyMatch(f -> f.getFacilityType() == type && !f.isDemo());
            if (realRecordExists) {
                return; // never shadow a record an administrator has verified
            }
            StationFacility f = new StationFacility(station, type, status);
            f.setDescription(description);
            f.setSource(DEMO_SOURCE);
            f.setDemo(true);
            facilities.save(f);
        });
    }

    private Optional<LineStation> node(String lineCode, String stationCode) {
        return lines.findByLineCodeIgnoreCase(lineCode)
                .flatMap(line -> stations.findByCode(stationCode)
                        .flatMap(station -> lineStations.findByLineIdAndStationId(line.getId(), station.getId())));
    }
}
