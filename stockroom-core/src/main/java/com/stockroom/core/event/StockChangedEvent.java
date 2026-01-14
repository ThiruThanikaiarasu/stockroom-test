package com.stockroom.core.event;

import java.time.LocalDateTime;

/** Raised after any movement changes on-hand or reserved quantity for a SKU. */
public record StockChangedEvent(Long skuId, String skuCode, String cause, LocalDateTime occurredAt) {
}
