package com.lhf.usercenter.model.request;

import lombok.Data;
import java.io.Serializable;

/**
 * 队伍修改请求体
 * @author Nick
 * @create 2023--24-14:28
 */
@Data
public class TeamJoinRequest implements Serializable {
    private static final long serialVersionUID = 941141758449488001L;

    /**
     * id
     */
    private Long teamId;

    /**
     * 密码
     */
    private String password;
}