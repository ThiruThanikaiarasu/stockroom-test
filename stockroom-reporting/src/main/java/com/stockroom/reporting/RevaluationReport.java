package com.stockroom.reporting;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RevaluationReport(String skuCode, List<Entry> entries) {

    public record Entry(LocalDateTime occurredAt, BigDecimal previousCost, BigDecimal newCost) {
    }
}
