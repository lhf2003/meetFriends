package com.lhf.usercenter.exception;

import com.lhf.usercenter.common.ErrorCode;

public class BusinessException extends RuntimeException {
    private int code;
    private String message;
    private String description;

    public BusinessException(int code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }

    public BusinessException(int code, String message) {
        this(code, message, "");
    }

    public BusinessException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(), "");
    }

    public BusinessException(ErrorCode errorCode, String description) {
        this(errorCode.getCode(), errorCode.getMessage(), description);
    }

    public BusinessException(ErrorCode errorCode, String message, String description) {
        this(errorCode.getCode(), message, description);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }
}