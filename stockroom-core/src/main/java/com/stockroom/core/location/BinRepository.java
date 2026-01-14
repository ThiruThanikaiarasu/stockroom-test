package com.stockroom.core.location;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BinRepository extends JpaRepository<Bin, Long> {

    List<Bin> findByLocationIdOrderByAisleAscBayAsc(Long locationId);

    Optional<Bin> findByLocationIdAndCode(Long locationId, String code);
}
