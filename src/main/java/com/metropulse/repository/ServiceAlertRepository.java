package com.metropulse.repository;

import com.metropulse.entity.ServiceAlert;
import com.metropulse.enums.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceAlertRepository extends JpaRepository<ServiceAlert, Long> {

    @EntityGraph(attributePaths = {"line", "station", "affectedEdges", "verifiedBy"})
    List<ServiceAlert> findByStatus(AlertStatus status);

    @EntityGraph(attributePaths = {"line", "station", "affectedEdges", "createdBy", "verifiedBy"})
    @Query("select a from ServiceAlert a where a.id = :id")
    Optional<ServiceAlert> findDetailed(@Param("id") Long id);

    @EntityGraph(attributePaths = {"line", "station", "createdBy", "verifiedBy"})
    Page<ServiceAlert> findByStatus(AlertStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"line", "station", "createdBy", "verifiedBy"})
    @Query("select a from ServiceAlert a")
    Page<ServiceAlert> findAllWithRefs(Pageable pageable);

    @EntityGraph(attributePaths = {"line", "station"})
    List<ServiceAlert> findTop5ByStatusOrderByUpdatedAtDesc(AlertStatus status);

    long countByStatus(AlertStatus status);

    boolean existsByDemoTrue();

    List<ServiceAlert> findByDemoTrue();
}
