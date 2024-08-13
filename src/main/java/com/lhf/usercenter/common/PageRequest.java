package com.lhf.usercenter.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageRequest implements Serializable {
    private static final long serialVersionUID = -4162304142710323660L;
    /**
     * 页码
     */
    protected int pageNum;
    /**
     * 页面大小
     */
    protected int pageSize;

}