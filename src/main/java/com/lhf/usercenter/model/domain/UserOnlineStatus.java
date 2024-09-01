package com.lhf.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户在线状态表
 * @TableName user_online_status
 */
@TableName(value ="user_online_status")
@Data
public class UserOnlineStatus implements Serializable {
    /**
     * 用户id
     */
    @TableId(value = "userId")
    private Long userId;

    /**
     * 状态，0-离线，1-在线
     */
    @TableField(value = "isOnline")
    private Integer isOnline;

    /**
     * 最后在线时间
     */
    @TableField(value = "lastOnline")
    private Date lastOnline;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}