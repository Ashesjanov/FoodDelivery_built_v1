package com.example.delivery.common;

/**
 * 业务规则失败使用的运行时异常，携带稳定的 {@link ErrorCode} 与可选自定义提示。
 * 通常由 service 或安全上下文抛出，再由 {@link GlobalExceptionHandler} 转换为统一响应。
 */
public class BizException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String errorMessage;

    public BizException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    public BizException(ErrorCode errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public BizException(ErrorCode errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
