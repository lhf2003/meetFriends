package com.lhf.usercenter.model.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReturnLocationBean implements Serializable {

    private static final Long serializableValue = 1L;


    /**
     * 地理位置
     */
    private String formattedAddress;


    /**
     * 经度
     */
    private Double lng;

    /**
     * 纬度
     */
    private Double lat;

    /**
     * 品级
     */
    private String level;
}
