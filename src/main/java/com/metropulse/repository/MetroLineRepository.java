package com.metropulse.repository;

import com.metropulse.entity.MetroLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetroLineRepository extends JpaRepository<MetroLine, Long> {

    Optional<MetroLine> findByLineCodeIgnoreCase(String lineCode);

    List<MetroLine> findAllByOrderByLineCodeAsc();
}
