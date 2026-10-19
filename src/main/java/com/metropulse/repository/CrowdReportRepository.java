package com.metropulse.repository;

import com.metropulse.entity.CrowdReport;
import com.metropulse.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface CrowdReportRepository extends JpaRepository<CrowdReport, Long>, JpaSpecificationExecutor<CrowdReport> {

    /** Reports that may influence routing now: not rejected and not yet expired. */
    @Query("""
            select r from CrowdReport r
            where r.verificationStatus <> com.metropulse.enums.VerificationStatus.REJECTED
              and r.expiresAt > :now
            """)
    List<CrowdReport> findEligible(@Param("now") Instant now);

    /** Non-rejected reports for the given nodes created after a cut-off (used for station status). */
    @Query("""
            select r from CrowdReport r
            where r.lineStation.id in :nodeIds
              and r.verificationStatus <> com.metropulse.enums.VerificationStatus.REJECTED
              and r.createdAt >= :since
            order by r.createdAt desc
            """)
    List<CrowdReport> findRecentForNodes(@Param("nodeIds") Collection<Long> nodeIds, @Param("since") Instant since);

    @EntityGraph(attributePaths = {"lineStation", "lineStation.line", "lineStation.station", "user", "verifiedBy"})
    Page<CrowdReport> findAll(Specification<CrowdReport> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"lineStation", "lineStation.line", "lineStation.station"})
    Page<CrowdReport> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndCreatedAtAfter(Long userId, Instant since);

    boolean existsByUserIdAndLineStationIdAndCreatedAtAfter(Long userId, Long lineStationId, Instant since);

    long countByVerificationStatus(VerificationStatus status);

    long countByCreatedAtAfter(Instant since);

    @Query("select r.crowdLevel, count(r) from CrowdReport r where r.createdAt >= :since group by r.crowdLevel")
    List<Object[]> countByLevelSince(@Param("since") Instant since);

    @Query("""
            select s.id, s.name, count(r) from CrowdReport r join r.lineStation ls join ls.station s
            where r.createdAt >= :since
            group by s.id, s.name
            order by count(r) desc, s.name
            """)
    List<Object[]> topStationsSince(@Param("since") Instant since, Pageable limit);

    boolean existsByDemoTrue();

    @Modifying
    @Query("delete from CrowdReport r where r.demo = true")
    int deleteDemo();
}
