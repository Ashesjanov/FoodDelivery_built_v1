package com.example.delivery.common;

import java.time.Instant;

/**
 * Stable envelope used by every REST endpoint.
 *
 * @param code    zero for success, or an {@link ErrorCode} value
 * @param message human-readable result message
 * @param data    response payload, or {@code null}
 * @param timestamp UTC timestamp when the envelope was created
 */
public record ApiResponse<T>(int code, String message, T data, Instant timestamp) {

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data, Instant.now());
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), "created", data, Instant.now());
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null, Instant.now());
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null, Instant.now());
    }
}
