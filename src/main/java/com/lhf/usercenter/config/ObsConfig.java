package com.lhf.usercenter.config;

import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;

/**
 * 华为obs配置类
 */
@Data
@Slf4j
@Configuration
public class ObsConfig {

    @Value("${huawei.obs.ak:home}")
    private String accessKey;
    @Value("${huawei.obs.sk:home}")
    private String securityKey;
    @Value("${huawei.obs.upload.endPoint:home}")
    private String endPoint;
    @Value("${huawei.obs.bucketName:home}")
    private String bucketName;
    @Value("${huawei.obs.urlPrefix:home}")
    private String urlPrefix;

    /**
     * 创建实例
     *
     * @return
     */
    public ObsClient getInstance() {
        return new ObsClient(accessKey, securityKey, endPoint);
    }

    /**
     * 销毁实列
     *
     * @param obsClient
     */
    public void destroy(ObsClient obsClient) {
        try {
            obsClient.close();
        } catch (ObsException e) {
            log.error("obs执行失败", e);
        } catch (Exception e) {
            log.error("执行失败", e);
        }
    }


}