package com.example.delivery.common;

import org.springframework.http.HttpStatus;

/**
 * 定义前后端共享的业务错误码、默认提示和默认 HTTP 状态。
 * service 用它抛出 {@link BizException}，全局异常处理和安全错误用它保持响应格式一致。
 * 错误码一经发布不可复用，新增错误应追加枚举值。
 */
public enum ErrorCode {
    SUCCESS(0, "success", HttpStatus.OK),
    BAD_REQUEST(40000, "request is invalid", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(40001, "request validation failed", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(40100, "authentication is required", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(40101, "username or password is incorrect", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(40102, "access token is invalid or expired", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(40300, "access is denied", HttpStatus.FORBIDDEN),
    NOT_FOUND(40400, "resource was not found", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(40401, "user was not found", HttpStatus.NOT_FOUND),
    ADDRESS_NOT_FOUND(40402, "address was not found", HttpStatus.NOT_FOUND),
    CONFLICT(40900, "resource conflicts with existing data", HttpStatus.CONFLICT),
    USER_ALREADY_EXISTS(40901, "username is already registered", HttpStatus.CONFLICT),
    ACCOUNT_DISABLED(40301, "account is disabled", HttpStatus.FORBIDDEN),
    INTERNAL_ERROR(50000, "internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
