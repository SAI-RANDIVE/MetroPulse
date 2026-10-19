package com.metropulse.repository;

import com.metropulse.entity.StationFacility;
import com.metropulse.enums.FacilityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StationFacilityRepository extends JpaRepository<StationFacility, Long> {

    List<StationFacility> findByStationIdOrderByFacilityType(Long stationId);

    @Query("select f from StationFacility f join fetch f.station")
    List<StationFacility> findAllWithStation();

    @EntityGraph(attributePaths = "station")
    @Query("""
            select f from StationFacility f
            where (:stationId is null or f.station.id = :stationId)
              and (:type is null or f.facilityType = :type)
            """)
    Page<StationFacility> search(@Param("stationId") Long stationId, @Param("type") FacilityType type, Pageable pageable);

    boolean existsByDemoTrue();

    @Modifying
    @Query("delete from StationFacility f where f.demo = true")
    int deleteDemo();
}
