package com.lhf.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterRequest implements Serializable {
    /**
     * 用户账号
     */
    private String userAccount;
    /**
     * 用户密码
     */
    private String userPassword;
    /**
     * 确认密码
     */
    private String checkPassword;
    /**
     * 验证方式
     */
    private String registerMethod;

    /**
     * 验证码
     */
    private String verifyCode;
    /**
     * 序列化
     */
    private static final long serialVersionUID = 1L;
}