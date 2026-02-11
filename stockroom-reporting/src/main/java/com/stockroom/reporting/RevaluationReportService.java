package com.stockroom.reporting;

import com.stockroom.core.costing.CostRevaluationRepository;
import com.stockroom.core.sku.Sku;
import com.stockroom.core.sku.SkuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RevaluationReportService {

    private final SkuService skus;
    private final CostRevaluationRepository revaluations;

    public RevaluationReportService(SkuService skus, CostRevaluationRepository revaluations) {
        this.skus = skus;
        this.revaluations = revaluations;
    }

    public RevaluationReport forSku(String skuCode) {
        Sku sku = skus.getByCode(skuCode);
        return new RevaluationReport(sku.getCode(), revaluations.findBySkuIdOrderByOccurredAtAscIdAsc(sku.getId())
                .stream()
                .map(r -> new RevaluationReport.Entry(r.getOccurredAt(), r.getPreviousCost(), r.getNewCost()))
                .toList());
    }
}
