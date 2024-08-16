package com.lhf.usercenter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
//                .allowedOrigins("http://139.159.143.140:8000", "http://meetfei.cn:8000", "http://www.meetfei.cn:8000")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    /**
     * Cookie 配置
     * @return CookieSerializer
     */
/*    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setDomainName(".meetfei.cn"); // 修改为实际域名
        serializer.setCookieName("SESSIONID");
        serializer.setCookieMaxAge(3600);
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(true); // For HTTPS
        serializer.setSameSite("None"); // For cross-site requests
        return serializer;
    }*/
}