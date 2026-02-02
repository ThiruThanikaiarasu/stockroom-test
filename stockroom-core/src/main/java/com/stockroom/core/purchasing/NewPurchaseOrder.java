package com.stockroom.core.purchasing;

import java.math.BigDecimal;
import java.util.List;

public record NewPurchaseOrder(String supplier, List<Line> lines) {

    public record Line(Long skuId, int quantity, BigDecimal unitCost) {
    }
}
