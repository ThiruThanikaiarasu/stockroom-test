package com.stockroom.api.error;

import java.util.List;

public record ApiError(int status, String code, String message, List<FieldProblem> fields) {

    public record FieldProblem(String field, String message) {
    }

    public static ApiError of(int status, String code, String message) {
        return new ApiError(status, code, message, List.of());
    }
}
