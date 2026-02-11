package com.stockroom.reporting;

import java.util.List;

/**
 * Units handled in a period. {@code adjustedUnits} is net (signed); every other figure is a count
 * of units physically handled.
 */
public record ThroughputReport(ReportPeriod period, int receivedUnits, int pickedUnits, int transferredUnits,
                               int adjustedUnits, List<SkuThroughput> bySku) {

    public record SkuThroughput(String skuCode, int unitsHandled, int movements) {
    }
}
