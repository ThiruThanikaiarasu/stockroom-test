package com.stockroom.reporting;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.stockroom.core.stock.StockMovement;
import com.stockroom.core.stock.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ThroughputReportService {

    private final StockMovementRepository movements;

    public ThroughputReportService(StockMovementRepository movements) {
        this.movements = movements;
    }

    public ThroughputReport throughput(ReportPeriod period) {
        Totals totals = new Totals();
        Map<String, int[]> bySku = new TreeMap<>();
        for (StockMovement movement : movements.findInPeriod(period.from(), period.to())) {
            int units = Math.abs(movement.getQuantity());
            switch (movement.getType()) {
                case RECEIPT -> totals.received += units;
                case PICK -> totals.picked += units;
                case ADJUSTMENT -> totals.adjusted += movement.getQuantity();
                case TRANSFER_OUT, TRANSFER_IN -> totals.transferred += units;
            }
            int[] sku = bySku.computeIfAbsent(movement.getSku().getCode(), code -> new int[2]);
            sku[0] += units;
            sku[1]++;
        }
        List<ThroughputReport.SkuThroughput> rows = bySku.entrySet().stream()
                .map(entry -> new ThroughputReport.SkuThroughput(entry.getKey(), entry.getValue()[0],
                        entry.getValue()[1]))
                .sorted(Comparator.comparingInt(ThroughputReport.SkuThroughput::unitsHandled).reversed())
                .toList();
        return new ThroughputReport(period, totals.received, totals.picked, totals.transferred, totals.adjusted,
                rows);
    }

    private static final class Totals {
        int received;
        int picked;
        int transferred;
        int adjusted;
    }
}
