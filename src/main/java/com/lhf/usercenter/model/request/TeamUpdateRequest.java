package com.lhf.usercenter.model.request;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 队伍修改请求体
 * @author Nick
 * @create 2023--24-14:28
 */
@Data
public class TeamUpdateRequest implements Serializable {
    private static final long serialVersionUID = 941141758449488001L;

    /**
     * id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 0 - 公开,1 - 私有,2 - 加密
     */
    private Integer status;

    /**
     * 密码
     */
    private String password;
}