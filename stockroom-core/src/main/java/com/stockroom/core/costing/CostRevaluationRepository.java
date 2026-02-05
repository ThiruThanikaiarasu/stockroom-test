package com.stockroom.core.costing;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CostRevaluationRepository extends JpaRepository<CostRevaluation, Long> {

    List<CostRevaluation> findBySkuIdOrderByOccurredAtAscIdAsc(Long skuId);
}
