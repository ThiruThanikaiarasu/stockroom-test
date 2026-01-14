package com.stockroom.core.sku;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuRepository extends JpaRepository<Sku, Long> {

    Optional<Sku> findByCode(String code);

    boolean existsByCode(String code);

    List<Sku> findByActiveTrueOrderByCodeAsc();
}
