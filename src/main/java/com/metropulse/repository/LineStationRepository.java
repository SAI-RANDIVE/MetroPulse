package com.metropulse.repository;

import com.metropulse.entity.LineStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LineStationRepository extends JpaRepository<LineStation, Long> {

    @Query("select ls from LineStation ls join fetch ls.line join fetch ls.station")
    List<LineStation> findAllWithLineAndStation();

    @Query("select ls from LineStation ls join fetch ls.line join fetch ls.station where ls.id = :id")
    Optional<LineStation> findWithLineAndStation(@Param("id") Long id);

    @Query("select ls from LineStation ls join fetch ls.station where ls.line.id = :lineId order by ls.sequenceNo")
    List<LineStation> findByLineOrdered(@Param("lineId") Long lineId);

    @Query("select ls from LineStation ls join fetch ls.line where ls.station.id = :stationId order by ls.line.lineCode")
    List<LineStation> findByStation(@Param("stationId") Long stationId);

    Optional<LineStation> findByLineIdAndStationId(Long lineId, Long stationId);

    boolean existsByLineIdAndStationId(Long lineId, Long stationId);

    @Query("select coalesce(max(ls.sequenceNo), 0) from LineStation ls where ls.line.id = :lineId")
    int maxSequence(@Param("lineId") Long lineId);

    @Query("select ls from LineStation ls where ls.line.id = :lineId and ls.sequenceNo = :seq")
    Optional<LineStation> findByLineAndSequence(@Param("lineId") Long lineId, @Param("seq") int sequenceNo);
}
