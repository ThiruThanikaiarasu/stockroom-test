package com.stockroom.core.sku;

public record NewSku(String code, String name, String description, String unitOfMeasure, int reorderPoint) {
}
