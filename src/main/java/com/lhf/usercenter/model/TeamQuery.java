package com.lhf.usercenter.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.lhf.usercenter.common.PageRequest;
import lombok.Data;

import java.util.Date;

@Data
public class TeamQuery extends PageRequest {

    private static final long serialVersionUID = 136976634690644209L;

    @TableId(type = IdType.AUTO)
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
     * 过期时间
     */
    private Date expireTime;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 0 - 公开,1 - 私有,2 - 加密
     */
    private Integer status;

    /**
     * 关键字
     */
    private String searchText;

}