package com.stockroom.reporting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.stockroom.core.sku.Sku;
import com.stockroom.core.sku.SkuRepository;
import com.stockroom.core.stock.StockLevelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Values on-hand stock at each SKU's current weighted average cost. */
@Service
@Transactional(readOnly = true)
public class ValuationReportService {

    private final SkuRepository skus;
    private final StockLevelRepository levels;

    public ValuationReportService(SkuRepository skus, StockLevelRepository levels) {
        this.skus = skus;
        this.levels = levels;
    }

    public ValuationReport valuation() {
        List<ValuationReport.Line> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (Sku sku : skus.findByActiveTrueOrderByCodeAsc()) {
            int onHand = levels.onHandForSku(sku.getId());
            if (onHand == 0) {
                continue;
            }
            BigDecimal value = sku.getAverageCost().multiply(BigDecimal.valueOf(onHand))
                    .setScale(2, RoundingMode.HALF_UP);
            lines.add(new ValuationReport.Line(sku.getCode(), sku.getName(), onHand, sku.getAverageCost(), value));
            total = total.add(value);
        }
        return new ValuationReport(lines, total);
    }
}
