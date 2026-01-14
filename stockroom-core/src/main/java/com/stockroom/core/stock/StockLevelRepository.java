package com.stockroom.core.stock;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {

    Optional<StockLevel> findBySkuIdAndBinId(Long skuId, Long binId);

    @Query("select l from StockLevel l join fetch l.bin b where l.sku.id = :skuId order by b.code")
    List<StockLevel> findBySkuId(Long skuId);

    @Query("select coalesce(sum(l.onHand - l.reserved), 0) from StockLevel l where l.sku.id = :skuId")
    int availableForSku(Long skuId);

    @Query("select coalesce(sum(l.onHand), 0) from StockLevel l where l.sku.id = :skuId")
    int onHandForSku(Long skuId);
}
