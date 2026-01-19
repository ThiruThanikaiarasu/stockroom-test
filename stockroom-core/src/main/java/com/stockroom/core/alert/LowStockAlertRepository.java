package com.stockroom.core.alert;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LowStockAlertRepository extends JpaRepository<LowStockAlert, Long> {

    boolean existsBySkuIdAndAcknowledgedFalse(Long skuId);

    @Query("select a from LowStockAlert a join fetch a.sku where a.acknowledged = false order by a.raisedAt, a.id")
    List<LowStockAlert> findOpen();

    List<LowStockAlert> findBySkuIdOrderByRaisedAtAsc(Long skuId);
}
