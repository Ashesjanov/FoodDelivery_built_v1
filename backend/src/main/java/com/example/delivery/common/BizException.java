package com.example.delivery.common;

/** Application exception carrying a stable API error identifier. */
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
