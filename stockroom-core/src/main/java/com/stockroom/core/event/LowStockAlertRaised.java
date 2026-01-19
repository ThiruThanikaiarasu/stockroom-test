package com.stockroom.core.event;

public record LowStockAlertRaised(Long alertId, Long skuId, String skuCode, int available, int reorderPoint) {
}
