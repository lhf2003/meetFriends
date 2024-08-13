package com.lhf.usercenter.common;

import lombok.Data;

@Data
public class BaseResponse<T> {
    /**
     * 状态码
     */
    private int code;
    /**
     * 返回信息
     */
    private String message;
    /**
     * 返回数据
     */
    private T data;
    /**
     * 描述（详细）信息
     */
    private String description;

    public BaseResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    public BaseResponse(int code, String message, T data, String description) {
        this(code, message, data);
        this.description = description;
    }
    public BaseResponse(int code, String message) {
        this(code, message, "");
    }
    public BaseResponse(int code, String message, String description) {
        this(code, message, null, description);
    }
    public BaseResponse() {

    }
}