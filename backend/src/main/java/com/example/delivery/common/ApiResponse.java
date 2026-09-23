package com.example.delivery.common;

import java.time.Instant;

/**
 * 所有 REST 接口共用的响应信封，也用于统一异常和安全错误输出。
 * 控制器通过静态工厂创建成功、创建成功和失败响应；业务代码应保持 code 稳定，供前端按值处理。
 *
 * @param code      成功时为 0，失败时为 {@link ErrorCode} 的错误码
 * @param message   面向调用方的结果提示
 * @param data      业务响应数据，无数据时为 {@code null}
 * @param timestamp 响应创建时的 UTC 时间
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
