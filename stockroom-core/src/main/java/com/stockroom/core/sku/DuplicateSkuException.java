package com.stockroom.core.sku;

public class DuplicateSkuException extends RuntimeException {

    public DuplicateSkuException(String code) {
        super("SKU " + code + " already exists");
    }
}
