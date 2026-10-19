package com.metropulse.service;

import com.metropulse.dto.LineBadge;
import com.metropulse.dto.LineSummary;
import com.metropulse.dto.form.LineExtensionForm;
import com.metropulse.entity.*;
import com.metropulse.enums.AuditAction;
import com.metropulse.enums.EdgeType;
import com.metropulse.exception.InvalidRequestException;
import com.metropulse.exception.ResourceNotFoundException;
import com.metropulse.repository.LineStationRepository;
import com.metropulse.repository.MetroLineRepository;
import com.metropulse.repository.NetworkEdgeRepository;
import com.metropulse.repository.StationRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Metro lines, their ordered stops, and admin changes to line membership and connections. */
@Service
@Transactional(readOnly = true)
public class LineService {

    private final MetroLineRepository lines;
    private final LineStationRepository lineStations;
    private final StationRepository stations;
    private final NetworkEdgeRepository edges;
    private final AuditService audit;
    private final EntityManager entityManager;

    public LineService(MetroLineRepository lines, LineStationRepository lineStations, StationRepository stations,
                       NetworkEdgeRepository edges, AuditService audit, EntityManager entityManager) {
        this.lines = lines;
        this.lineStations = lineStations;
        this.stations = stations;
        this.edges = edges;
        this.audit = audit;
        this.entityManager = entityManager;
    }

    public List<LineSummary> listAll() {
        List<LineStation> all = lineStations.findAllWithLineAndStation();
        Map<Long, List<LineBadge>> linesByStation = all.stream()
                .collect(Collectors.groupingBy(ls -> ls.getStation().getId(),
                        Collectors.mapping(ls -> LineBadge.of(ls.getLine()), Collectors.toList())));
        return lines.findAllByOrderByLineCodeAsc().stream()
                .map(line -> toSummary(line, all, linesByStation))
                .toList();
    }

    public LineSummary byCode(String code) {
        MetroLine line = lines.findByLineCodeIgnoreCase(code).orElseThrow(() -> new ResourceNotFoundException("Line", code));
        return listAll().stream().filter(l -> l.id() == line.getId()).findFirst().orElseThrow();
    }

    public MetroLine require(long lineId) {
        return lines.findById(lineId).orElseThrow(() -> new ResourceNotFoundException("Line", lineId));
    }

    public List<LineBadge> badges() {
        return lines.findAllByOrderByLineCodeAsc().stream().map(LineBadge::of).toList();
    }

    /** Ride connections of a line (both directions) for the admin network page. */
    public List<NetworkEdge> rideEdges(long lineId) {
        return edges.findRideEdgesOfLine(lineId);
    }

    private static LineSummary toSummary(MetroLine line, List<LineStation> all, Map<Long, List<LineBadge>> linesByStation) {
        List<LineSummary.LineStop> stops = all.stream()
                .filter(ls -> ls.getLine().getId().equals(line.getId()))
                .sorted(Comparator.comparingInt(LineStation::getSequenceNo))
                .map(ls -> new LineSummary.LineStop(ls.getId(), ls.getStation().getId(), ls.getStation().getName(),
                        ls.getSequenceNo(), ls.isOperational(),
                        linesByStation.getOrDefault(ls.getStation().getId(), List.of()).stream()
                                .filter(b -> b.id() != line.getId()).toList()))
                .toList();
        return new LineSummary(line.getId(), line.getLineCode(), line.getName(), line.getColor(), line.getTextColor(),
                line.getOperatorName(), line.getStatus(), stops);
    }

    // ---------- admin ----------

    /** Non-operational = trains pass without stopping. The node stays in the graph so the line is not cut. */
    @Transactional
    public void setOperational(long lineStationId, boolean operational, AppUser admin) {
        LineStation node = lineStations.findWithLineAndStation(lineStationId)
                .orElseThrow(() -> new ResourceNotFoundException("Station on line", lineStationId));
        node.setOperational(operational);
        audit.record(admin, AuditAction.LINE_STATION_UPDATED, "LineStation", lineStationId,
                node.getStation().getName() + " on " + node.getLine().getShortName() + " operational=" + operational);
    }

    /**
     * Adds a station at the start or end of a line and creates the two directed ride edges to its neighbour,
     * all in one transaction so the graph never sees a half-connected stop.
     */
    @Transactional
    public LineStation extend(long lineId, LineExtensionForm form, AppUser admin) {
        MetroLine line = require(lineId);
        Station station = stations.findById(form.getStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station", form.getStationId()));
        if (lineStations.existsByLineIdAndStationId(lineId, station.getId())) {
            throw new InvalidRequestException(station.getName() + " is already on " + line.getShortName());
        }
        int last = lineStations.maxSequence(lineId);
        LineStation neighbour;
        LineStation added;
        if (last == 0) {
            added = lineStations.save(new LineStation(line, station, 1));
            neighbour = null;
        } else if (form.getEnd() == LineExtensionForm.End.END) {
            neighbour = lineStations.findByLineAndSequence(lineId, last).orElseThrow();
            added = lineStations.save(new LineStation(line, station, last + 1));
        } else {
            shiftSequencesUp(lineId);
            neighbour = lineStations.findByLineAndSequence(lineId, 2).orElseThrow();
            added = lineStations.save(new LineStation(line, station, 1));
        }
        if (neighbour != null) {
            edges.save(new NetworkEdge(neighbour, added, EdgeType.RIDE, form.getMinutes()));
            edges.save(new NetworkEdge(added, neighbour, EdgeType.RIDE, form.getMinutes()));
        }
        audit.record(admin, AuditAction.LINE_STATION_ADDED, "LineStation", added.getId(),
                station.getName() + " added at the " + form.getEnd().name().toLowerCase() + " of " + line.getShortName());
        return added;
    }

    /** Moves every stop of the line one position later. Done in two steps so the unique (line, sequence) key holds. */
    private void shiftSequencesUp(long lineId) {
        entityManager.flush();
        entityManager.createQuery("update LineStation ls set ls.sequenceNo = ls.sequenceNo + 10000 where ls.line.id = :id")
                .setParameter("id", lineId).executeUpdate();
        entityManager.createQuery("update LineStation ls set ls.sequenceNo = ls.sequenceNo - 9999 where ls.line.id = :id")
                .setParameter("id", lineId).executeUpdate();
        entityManager.clear();
    }

    @Transactional
    public void updateEdge(long edgeId, int minutes, boolean active, AppUser admin) {
        if (minutes < 0 || minutes > 60) {
            throw new InvalidRequestException("Travel time must be between 0 and 60 minutes");
        }
        NetworkEdge edge = edges.findById(edgeId).orElseThrow(() -> new ResourceNotFoundException("Connection", edgeId));
        String before = edge.getBaseMinutes() + " min, active=" + edge.isActive();
        edge.setBaseMinutes(minutes);
        edge.setActive(active);
        audit.record(admin, AuditAction.EDGE_UPDATED, "NetworkEdge", edgeId,
                before + " -> " + minutes + " min, active=" + active);
    }
}
