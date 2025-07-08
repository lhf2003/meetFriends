package com.lhf.usercenter.common;

import com.lhf.usercenter.common.contant.CommonConstant;
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
    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认升序）
     */
    private String sortOrder = CommonConstant.SORT_ORDER_ASC;

}