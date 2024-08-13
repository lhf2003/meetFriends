package com.lhf.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = 6424766555807930307L;
    /**
     * 用户id
     */
    Long id;
}