package com.stockroom.reporting;

import java.math.BigDecimal;
import java.util.List;

public record ValuationReport(List<Line> lines, BigDecimal totalValue) {

    public record Line(String skuCode, String name, int onHand, BigDecimal averageCost, BigDecimal value) {
    }
}
