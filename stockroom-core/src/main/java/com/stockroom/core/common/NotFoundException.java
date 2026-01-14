package com.stockroom.core.common;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String entity, Object id) {
        super(entity + " " + id + " not found");
    }
}
