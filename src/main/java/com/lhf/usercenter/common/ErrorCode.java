package com.lhf.usercenter.common;

/**
 * 枚举类
 *
 * @Author: lhf
 */
public enum ErrorCode {
    PARAM_ERROR(40000, "请求参数错误"),
    USER_NOT_FOUND(40002, "用户不存在"),
    USER_EXIST(40003, "用户已存在"),
    USER_NOT_LOGIN(40100, "用户未登录"),
    NULL_DATA(40004, "数据为空"),
    AUTH_ERROR(40005, "权限不足"),
    SUCCESS(0, "操作成功"),
    ERROR(50000, "操作失败"),
    SEND_MAIL_ERROR(50001, "发送邮件失败");

    private int code;
    private String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}