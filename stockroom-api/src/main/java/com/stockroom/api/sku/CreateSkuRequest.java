package com.stockroom.api.sku;

import com.stockroom.core.sku.NewSku;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateSkuRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotBlank @Size(max = 10) String unitOfMeasure,
        @PositiveOrZero int reorderPoint) {

    NewSku toNewSku() {
        return new NewSku(code, name, description, unitOfMeasure, reorderPoint);
    }
}
