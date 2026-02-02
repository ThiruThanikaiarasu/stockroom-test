package com.stockroom.core.purchasing;

import java.math.BigDecimal;

/**
 * One received line of a purchase order.
 *
 * @param invoicedUnitCost the supplier's invoiced cost when it differs from the order; null to use
 *                         the ordered unit cost
 */
public record ReceiptLine(Long lineId, Long binId, int quantity, BigDecimal invoicedUnitCost) {
}
