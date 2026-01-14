package com.stockroom.core.stock;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query("""
            select m from StockMovement m join fetch m.sku join fetch m.bin
            where m.occurredAt >= :from and m.occurredAt < :to
            order by m.occurredAt, m.id
            """)
    List<StockMovement> findInPeriod(LocalDateTime from, LocalDateTime to);

    List<StockMovement> findBySkuIdOrderByOccurredAtAscIdAsc(Long skuId);

    List<StockMovement> findByTransferId(String transferId);
}
