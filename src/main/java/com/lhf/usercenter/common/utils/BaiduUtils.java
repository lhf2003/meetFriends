package com.lhf.usercenter.common.utils;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.lhf.usercenter.model.domain.ReturnLocationBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
public class BaiduUtils {

    private static String ak = "Xxt5idmN3iqhNP0dxuN22Z5KMoDnK1me";

    private static final String GEOCODING_URL = "http://api.map.baidu.com/geocoding/v3/?output=json&location=showLocation";

    /**
     * 地理编码
     *
     * @param address (如: 广东省广州市黄埔区)
     * @return 详细的位置信息
     */
    public static ReturnLocationBean addressToLongitude(String address) {
        if (StringUtils.isBlank(address)) {
            return null;
        }

        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8.toString());
            String url = GEOCODING_URL + "&ak=" + ak + "&address=" + encodedAddress;
            log.info("请求URL: " + url);

            HttpClient client = HttpClients.createDefault();
            HttpGet get = new HttpGet(url);

            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            JSONObject jsonObject = JSONUtil.parseObj(responseBody);

            // 检查 API 调用是否成功
            int status = jsonObject.getInt("status");
            if (status != 0) {
                log.error("百度地图 API 错误, 状态码: " + status);
                return null;
            }

            // 封装地理位置信息
            ReturnLocationBean locationBean = new ReturnLocationBean();
            locationBean.setLng(jsonObject.getJSONObject("result").getJSONObject("location").getDouble("lng"));
            locationBean.setLat(jsonObject.getJSONObject("result").getJSONObject("location").getDouble("lat"));
            locationBean.setLevel(jsonObject.getJSONObject("result").getStr("level"));
            locationBean.setFormattedAddress(address);
            return locationBean;
        } catch (Exception e) {
            log.error("地理编码[异常]: ", e);
            return null;
        }
    }
}
