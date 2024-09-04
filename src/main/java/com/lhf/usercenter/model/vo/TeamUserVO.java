package com.lhf.usercenter.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 队伍和用户封装类（脱敏）
 */
@Data
public class TeamUserVO implements Serializable {
    private static final long serialVersionUID = -4432828954813281541L;

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
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 已加入人数
     */
    private Integer hasJoinNum;

    /**
     * 是否加入队伍
     */
    private Boolean hasJoin;

    /**
     * 过期时间
     */
    private String expireTime;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 0 - 公开,1 - 私有,2 - 加密
     */
    private Integer status;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 更新时间
     */
    private String updateTime;

    /**
     * 队伍创建人用户信息
     */
    UserVO createdUser;
}