package com.metropulse.repository;

import com.metropulse.entity.Station;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {

    List<Station> findAllByOrderByNameAsc();

    Page<Station> findAllByOrderByNameAsc(Pageable pageable);

    Optional<Station> findByCode(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Partial, punctuation-insensitive search: "dn nagar" finds "D N Nagar", "andheri w" finds "Andheri (West)".
     * The caller passes the query already reduced to lowercase letters and digits.
     */
    @Query(value = """
            SELECT * FROM station s
            WHERE regexp_replace(lower(s.name), '[^a-z0-9]', '', 'g') LIKE '%' || :q || '%'
               OR regexp_replace(lower(coalesce(s.area, '')), '[^a-z0-9]', '', 'g') LIKE '%' || :q || '%'
            ORDER BY CASE WHEN regexp_replace(lower(s.name), '[^a-z0-9]', '', 'g') LIKE :q || '%' THEN 0 ELSE 1 END,
                     s.name
            LIMIT :limit
            """, nativeQuery = true)
    List<Station> searchNormalized(@Param("q") String normalizedQuery, @Param("limit") int limit);
}
