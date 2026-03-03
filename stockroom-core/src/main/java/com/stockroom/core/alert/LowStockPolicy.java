package com.stockroom.core.alert;

import org.springframework.stereotype.Component;

/** Decides when a SKU's available stock is low enough to alert purchasing. */
@Component
public class LowStockPolicy {

    public boolean isLow(int available, int reorderPoint) {
        if (reorderPoint <= 0) {
            return false;
        }
        return reorderPoint >= available;
    }
}
