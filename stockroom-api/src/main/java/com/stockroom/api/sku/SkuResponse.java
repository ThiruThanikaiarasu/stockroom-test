package com.stockroom.api.sku;

import com.stockroom.core.sku.Sku;

public record SkuResponse(Long id, String code, String name, String description, String unitOfMeasure,
                          int reorderPoint, boolean active) {

    public static SkuResponse from(Sku sku) {
        return new SkuResponse(sku.getId(), sku.getCode(), sku.getName(), sku.getDescription(),
                sku.getUnitOfMeasure(), sku.getReorderPoint(), sku.isActive());
    }
}
