package com.metropulse.repository;

import com.metropulse.entity.NetworkEdge;
import com.metropulse.enums.EdgeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NetworkEdgeRepository extends JpaRepository<NetworkEdge, Long> {

    @Query("select e from NetworkEdge e join fetch e.fromNode join fetch e.toNode")
    List<NetworkEdge> findAllWithNodes();

    Optional<NetworkEdge> findByFromNodeIdAndToNodeIdAndEdgeType(Long fromNodeId, Long toNodeId, EdgeType type);

    /** Ride edges between stops whose sequence numbers lie within [fromSeq, toSeq] on one line, both directions. */
    @Query("""
            select e from NetworkEdge e
            join fetch e.fromNode f join fetch e.toNode t
            where e.edgeType = com.metropulse.enums.EdgeType.RIDE
              and f.line.id = :lineId and t.line.id = :lineId
              and f.sequenceNo between :fromSeq and :toSeq
              and t.sequenceNo between :fromSeq and :toSeq
            """)
    List<NetworkEdge> findRideEdgesInSegment(@Param("lineId") Long lineId,
                                             @Param("fromSeq") int fromSeq,
                                             @Param("toSeq") int toSeq);

    @Query("""
            select e from NetworkEdge e
            join fetch e.fromNode f join fetch f.station join fetch e.toNode t join fetch t.station
            where e.edgeType = com.metropulse.enums.EdgeType.RIDE and f.line.id = :lineId
            order by f.sequenceNo, t.sequenceNo
            """)
    List<NetworkEdge> findRideEdgesOfLine(@Param("lineId") Long lineId);

    /** Transfer edges leaving any node of a station, with the destination line and station loaded. */
    @Query("""
            select e from NetworkEdge e
            join fetch e.fromNode f join fetch e.toNode t join fetch t.line join fetch t.station
            where e.edgeType = com.metropulse.enums.EdgeType.TRANSFER and f.station.id = :stationId
            """)
    List<NetworkEdge> findTransfersFromStation(@Param("stationId") Long stationId);

    /** Stations that have at least one active modelled interchange. */
    @Query("""
            select distinct f.station.id from NetworkEdge e join e.fromNode f
            where e.edgeType = com.metropulse.enums.EdgeType.TRANSFER and e.active = true
            """)
    List<Long> findInterchangeStationIds();
}
