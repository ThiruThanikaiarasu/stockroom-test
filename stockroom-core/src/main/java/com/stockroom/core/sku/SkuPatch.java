package com.stockroom.core.sku;

/**
 * Partial update of a SKU. A null {@code name}, {@code unitOfMeasure} or {@code reorderPoint}
 * leaves the current value in place.
 */
public record SkuPatch(String name, String description, String unitOfMeasure, Integer reorderPoint, Boolean active) {
}
