package com.metropulse.service;

import com.metropulse.dto.*;
import com.metropulse.dto.form.StationForm;
import com.metropulse.entity.AppUser;
import com.metropulse.entity.LineStation;
import com.metropulse.entity.NetworkEdge;
import com.metropulse.entity.Station;
import com.metropulse.enums.AuditAction;
import com.metropulse.exception.InvalidRequestException;
import com.metropulse.exception.ResourceNotFoundException;
import com.metropulse.repository.LineStationRepository;
import com.metropulse.repository.NetworkEdgeRepository;
import com.metropulse.repository.StationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/** Station directory, search, station pages and admin station edits. */
@Service
@Transactional(readOnly = true)
public class StationService {

    public static final int SEARCH_LIMIT = 12;

    private final StationRepository stations;
    private final LineStationRepository lineStations;
    private final NetworkEdgeRepository edges;
    private final CrowdStatusService crowdStatus;
    private final CrowdReportService crowdReports;
    private final FacilityService facilities;
    private final ServiceAlertService alerts;
    private final AuditService audit;

    public StationService(StationRepository stations, LineStationRepository lineStations, NetworkEdgeRepository edges,
                          CrowdStatusService crowdStatus, CrowdReportService crowdReports, FacilityService facilities,
                          ServiceAlertService alerts, AuditService audit) {
        this.stations = stations;
        this.lineStations = lineStations;
        this.edges = edges;
        this.crowdStatus = crowdStatus;
        this.crowdReports = crowdReports;
        this.facilities = facilities;
        this.alerts = alerts;
        this.audit = audit;
    }

    /** All stations A-Z with their line badges. */
    public List<StationSummary> listAll() {
        Map<Long, List<LineBadge>> badges = badgesByStation();
        return stations.findAllByOrderByNameAsc().stream()
                .map(s -> summary(s, badges.getOrDefault(s.getId(), List.of())))
                .toList();
    }

    /** Partial, case- and punctuation-insensitive search on station name and area. */
    public List<StationSummary> search(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return listAll();
        }
        Map<Long, List<LineBadge>> badges = badgesByStation();
        return stations.searchNormalized(normalized, SEARCH_LIMIT).stream()
                .map(s -> summary(s, badges.getOrDefault(s.getId(), List.of())))
                .toList();
    }

    /** Interchange stations, shown as quick picks on the home page. */
    public List<StationSummary> interchanges() {
        Set<Long> ids = new HashSet<>(edges.findInterchangeStationIds());
        return listAll().stream().filter(s -> ids.contains(s.id())).toList();
    }

    public static String normalize(String query) {
        return query == null ? "" : query.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public Station require(long id) {
        return stations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Station", id));
    }

    public StationSummary summary(long id) {
        Station station = require(id);
        List<LineBadge> lines = lineStations.findByStation(id).stream().map(ls -> LineBadge.of(ls.getLine())).toList();
        return summary(station, lines);
    }

    public StationStatusView status(long id) {
        Station station = require(id);
        List<LineStation> nodes = lineStations.findByStation(id);
        return status(station, nodes);
    }

    public StationDetailView detail(long id) {
        Station station = require(id);
        List<LineStation> nodes = lineStations.findByStation(id);
        List<LineBadge> lines = nodes.stream().map(ls -> LineBadge.of(ls.getLine())).toList();

        List<StationDetailView.Membership> memberships = new ArrayList<>();
        for (LineStation node : nodes) {
            Long lineId = node.getLine().getId();
            StationRef previous = neighbour(lineId, node.getSequenceNo() - 1);
            StationRef next = neighbour(lineId, node.getSequenceNo() + 1);
            memberships.add(new StationDetailView.Membership(node.getId(), LineBadge.of(node.getLine()),
                    node.getSequenceNo(), node.isOperational(), previous, next));
        }

        Map<Long, LineStation> nodeById = nodes.stream().collect(Collectors.toMap(LineStation::getId, n -> n));
        List<StationDetailView.Interchange> interchanges = edges.findTransfersFromStation(id).stream()
                .filter(NetworkEdge::isActive)
                .map(e -> new StationDetailView.Interchange(
                        new StationRef(e.getToNode().getStation().getId(), e.getToNode().getStation().getName()),
                        LineBadge.of(nodeById.get(e.getFromNode().getId()).getLine()),
                        LineBadge.of(e.getToNode().getLine()), e.getBaseMinutes(), e.getNote()))
                .toList();

        return new StationDetailView(summary(station, lines), memberships, interchanges, status(station, nodes),
                crowdReports.recentForStation(nodes, 8));
    }

    private StationStatusView status(Station station, List<LineStation> nodes) {
        Set<Long> lineIds = nodes.stream().map(n -> n.getLine().getId()).collect(Collectors.toSet());
        Set<Long> nodeIds = nodes.stream().map(LineStation::getId).collect(Collectors.toSet());
        return new StationStatusView(new StationRef(station.getId(), station.getName()),
                crowdStatus.statusFor(nodes),
                alerts.affectingStation(station.getId(), lineIds, nodeIds),
                facilities.accessibilityOf(station.getId()),
                facilities.forStation(station.getId()));
    }

    private StationRef neighbour(Long lineId, int sequenceNo) {
        return lineStations.findByLineAndSequence(lineId, sequenceNo)
                .map(ls -> new StationRef(ls.getStation().getId(), ls.getStation().getName()))
                .orElse(null);
    }

    /** Station-line nodes for the crowd-report form, grouped by station name. */
    public List<LineStation> allNodes() {
        return lineStations.findAllWithLineAndStation().stream()
                .sorted(Comparator.comparing((LineStation ls) -> ls.getStation().getName())
                        .thenComparing(ls -> ls.getLine().getLineCode()))
                .toList();
    }

    // ---------- admin ----------

    public StationForm toForm(long id) {
        Station s = require(id);
        StationForm form = new StationForm();
        form.setCode(s.getCode());
        form.setName(s.getName());
        form.setArea(s.getArea());
        form.setLatitude(s.getLatitude());
        form.setLongitude(s.getLongitude());
        form.setStatus(s.getStatus());
        return form;
    }

    @Transactional
    public Station create(StationForm form, AppUser admin) {
        if (stations.findByCode(form.getCode()).isPresent()) {
            throw new InvalidRequestException("Station code " + form.getCode() + " is already used");
        }
        if (stations.existsByNameIgnoreCase(form.getName().strip())) {
            throw new InvalidRequestException("A station called " + form.getName().strip() + " already exists");
        }
        Station station = new Station(form.getCode(), form.getName().strip(), FacilityService.blankToNull(form.getArea()), form.getStatus());
        station.setLatitude(form.getLatitude());
        station.setLongitude(form.getLongitude());
        stations.save(station);
        audit.record(admin, AuditAction.STATION_CREATED, "Station", station.getId(), station.getName());
        return station;
    }

    /** Updates a station. The code is a stable identifier and is not changed here. */
    @Transactional
    public Station update(long id, StationForm form, AppUser admin) {
        Station station = require(id);
        if (stations.existsByNameIgnoreCaseAndIdNot(form.getName().strip(), id)) {
            throw new InvalidRequestException("A station called " + form.getName().strip() + " already exists");
        }
        String before = station.getName() + " / " + station.getStatus();
        station.setName(form.getName().strip());
        station.setArea(FacilityService.blankToNull(form.getArea()));
        station.setLatitude(form.getLatitude());
        station.setLongitude(form.getLongitude());
        station.setStatus(form.getStatus());
        audit.record(admin, AuditAction.STATION_UPDATED, "Station", id, before + " -> " + station.getName() + " / " + station.getStatus());
        return station;
    }

    private Map<Long, List<LineBadge>> badgesByStation() {
        return lineStations.findAllWithLineAndStation().stream()
                .sorted(Comparator.comparing(ls -> ls.getLine().getLineCode()))
                .collect(Collectors.groupingBy(ls -> ls.getStation().getId(),
                        Collectors.mapping(ls -> LineBadge.of(ls.getLine()), Collectors.toList())));
    }

    private static StationSummary summary(Station s, List<LineBadge> lines) {
        return new StationSummary(s.getId(), s.getCode(), s.getName(), s.getArea(), s.getStatus(), lines);
    }
}
