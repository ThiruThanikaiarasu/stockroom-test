package com.stockroom.api.sku;

import com.stockroom.core.sku.SkuPatch;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PatchSkuRequest(
        @Size(min = 1, max = 120) String name,
        @Size(max = 500) String description,
        @Size(min = 1, max = 10) String unitOfMeasure,
        @PositiveOrZero Integer reorderPoint,
        Boolean active) {

    SkuPatch toPatch() {
        return new SkuPatch(name, description, unitOfMeasure, reorderPoint, active);
    }
}
