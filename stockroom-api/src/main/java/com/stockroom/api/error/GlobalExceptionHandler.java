package com.stockroom.api.error;

import java.util.List;

import com.stockroom.core.common.InsufficientStockException;
import com.stockroom.core.common.InvalidStateException;
import com.stockroom.core.common.NotFoundException;
import com.stockroom.core.sku.DuplicateSkuException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    ResponseEntity<ApiError> insufficient(InsufficientStockException e) {
        return respond(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", e.getMessage());
    }

    @ExceptionHandler({InvalidStateException.class, DuplicateSkuException.class})
    ResponseEntity<ApiError> conflict(RuntimeException e) {
        return respond(HttpStatus.CONFLICT, "CONFLICT", e.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> badRequest(Exception e) {
        return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalid(MethodArgumentNotValidException e) {
        List<ApiError.FieldProblem> fields = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldProblem(error.getField(), error.getDefaultMessage()))
                .toList();
        ApiError body = new ApiError(400, "VALIDATION_FAILED", "Request validation failed", fields);
        return ResponseEntity.badRequest().body(body);
    }

    private static ResponseEntity<ApiError> respond(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), code, message));
    }
}
