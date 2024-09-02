package com.lhf.usercenter.model.request;

import lombok.Data;

@Data
public class UserModifyPasswordRequest {
    /**
     * 旧密码
     */
    private String oldPassword;
    /**
     * 新密码
     */
    private String newPassword;
    /**
     * 确认新密码
     */
    private String checkPassword;
}