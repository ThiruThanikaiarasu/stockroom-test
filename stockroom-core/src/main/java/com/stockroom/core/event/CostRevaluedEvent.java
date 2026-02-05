package com.stockroom.core.event;

import java.math.BigDecimal;

public record CostRevaluedEvent(Long skuId, BigDecimal previousCost, BigDecimal newCost) {
}
